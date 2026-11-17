package com.bigstock.biz.service;

import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import com.bigstock.biz.component.LocalSessionRegistry;
import com.bigstock.biz.component.WebSocketAttributes;
import com.bigstock.biz.service.WebQuoteVisitor.SocketIdentity;
import com.bigstock.biz.service.MarketQuoteEvents.Update;
import com.bigstock.biz.service.MarketQuotesService.Quote;
import com.bigstock.biz.service.MarketQuotesService.Snapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MarketQuoteWebSocketService {

    public static final String DEFAULT_DESTINATION = "/topic/marketQuotes/default";
    public static final String SUBSCRIBED_DESTINATION = "/topic/marketQuotes/subscribed";
    public static final String WEB_DESTINATION = "/topic/marketQuotes/web";

    private final MarketQuotesService quotes;
    private final MarketQuoteSubscriptions subscriptions;
    private final LocalSessionRegistry sessions;
    private final ObjectMapper mapper;
    private final MessageChannel outbound;
    private final WebQuoteVisitor visitors;
    private final RedissonClient redisson;
    private final Map<SubscriptionId, Subscription> listeners = new ConcurrentHashMap<>();

    public MarketQuoteWebSocketService(MarketQuotesService quotes, MarketQuoteSubscriptions subscriptions,
            LocalSessionRegistry sessions, ObjectMapper mapper, WebQuoteVisitor visitors, RedissonClient redisson,
            @Qualifier("clientOutboundChannel") MessageChannel outbound) {
        this.quotes = quotes;
        this.subscriptions = subscriptions;
        this.sessions = sessions;
        this.mapper = mapper;
        this.outbound = outbound;
        this.visitors = visitors;
        this.redisson = redisson;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String destination = headers.getDestination();
        if (!DEFAULT_DESTINATION.equals(destination) && !SUBSCRIBED_DESTINATION.equals(destination)
                && !WEB_DESTINATION.equals(destination)) {
            return;
        }

        Principal user = event.getUser();
        String sessionId = headers.getSessionId();
        String subscriptionId = headers.getSubscriptionId();
        if (user == null || sessionId == null || subscriptionId == null) {
            return;
        }
        var id = new SubscriptionId(sessionId, subscriptionId);
        var attributes = headers.getSessionAttributes();
        var webIdentity = attributes == null ? null : attributes.get(WebSocketAttributes.WEB_QUOTE_IDENTITY);
        if (WEB_DESTINATION.equals(destination) && !(webIdentity instanceof SocketIdentity)) {
            return;
        }
        var subscription = new Subscription(user.getName(), destination,
                WEB_DESTINATION.equals(destination) ? (SocketIdentity) webIdentity : null);
        listeners.put(id, subscription);
        if (!sessions.isOpen(sessionId)) {
            listeners.remove(id, subscription);
            return;
        }

        try {
            Selection selection = select(subscription, new HashMap<>());
            for (String source : MarketQuotesService.SOURCES) {
                send(id, subscription, source, index(quotes.readSnapshot(source)), selection);
            }
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not send the initial market quote snapshot", ex);
        }
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        listeners.remove(new SubscriptionId(headers.getSessionId(), headers.getSubscriptionId()));
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        listeners.keySet().removeIf(id -> event.getSessionId().equals(id.sessionId()));
    }

    public void onRedisUpdate(byte[] body) {
        if (listeners.isEmpty()) {
            return;
        }
        try {
            Update update = mapper.readValue(body, Update.class);
            if (update.userId() != null) {
                pushSubscriptionsChanged(update.userId());
            } else if (MarketQuotesService.SOURCES.contains(update.source()) && update.snapshot() != null) {
                pushQuotesUpdated(update.source(), update.snapshot());
            }
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not forward market quote update to WebSocket clients", ex);
        }
    }

    private void pushQuotesUpdated(String source, Snapshot snapshot) {
        IndexedSnapshot indexed = index(snapshot);
        Map<String, List<String>> codesByUser = new HashMap<>();
        listeners.forEach((id, subscription) -> {
            send(id, subscription, source, indexed, select(subscription, codesByUser));
        });
    }

    private void pushSubscriptionsChanged(String userId) throws IOException {
        var targets = listeners.entrySet().stream()
                .filter(entry -> userId.equals(entry.getValue().owner()))
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        Map<String, List<String>> codesByUser = new HashMap<>();
        for (String source : MarketQuotesService.SOURCES) {
            IndexedSnapshot snapshot = index(quotes.readSnapshot(source));
            targets.forEach(entry -> {
                Subscription subscription = entry.getValue();
                send(entry.getKey(), subscription, source, snapshot, select(subscription, codesByUser));
            });
        }
    }

    private Selection select(Subscription subscription, Map<String, List<String>> codesByUser) {
        SocketIdentity web = subscription.web();
        if (web == null) {
            String owner = subscription.owner();
            List<String> codes = owner == null ? MarketQuoteSubscriptions.DEFAULT_STOCKS
                    : codesByUser.computeIfAbsent(owner, subscriptions::stockCodes);
            return new Selection(codes, false);
        }
        if (web.visitor() == null) {
            return new Selection(MarketQuoteSubscriptions.DEFAULT_STOCKS, false);
        }
        try {
            if (!web.expiresAt().isAfter(Instant.now())
                    || !redisson.getBucket("web:jwt:" + subscription.userId()).isExists()) {
                return new Selection(MarketQuoteSubscriptions.DEFAULT_STOCKS, true);
            }
            if (visitors.isCurrent(web.visitor())) {
                List<String> codes = codesByUser.computeIfAbsent(web.visitor().owner(), subscriptions::stockCodes);
                if (!codes.isEmpty()) {
                    return new Selection(codes, false);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Could not authorize private web quote selection", ex);
        }
        return new Selection(MarketQuoteSubscriptions.DEFAULT_STOCKS, false);
    }

    private IndexedSnapshot index(Snapshot snapshot) {
        if (snapshot == null) {
            return new IndexedSnapshot(null, Map.of());
        }
        Map<String, Quote> byCode = new HashMap<>();
        for (Quote quote : snapshot.items()) {
            byCode.put(quote.name(), quote);
        }
        return new IndexedSnapshot(snapshot.fetchedAt(), byCode);
    }

    private void send(SubscriptionId id, Subscription subscription, String source, IndexedSnapshot snapshot,
            Selection selection) {
        if (listeners.get(id) != subscription || !sessions.isOpen(id.sessionId())) {
            return;
        }
        List<String> codes = selection.codes();
        List<Quote> items = codes.stream().filter(snapshot.quotes()::containsKey)
                .map(snapshot.quotes()::get).toList();
        try {
            Object message = subscription.web() == null ? new QuoteMessage(source, snapshot.fetchedAt(), codes, items)
                    : new WebQuoteMessage(source, snapshot.fetchedAt(), codes, items, selection.reconnectRequired());
            byte[] payload = mapper.writeValueAsBytes(message);
            // Address this STOMP subscription directly instead of broadcasting to the broker topic.
            SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headers.setSessionId(id.sessionId());
            headers.setSubscriptionId(id.subscriptionId());
            headers.setDestination(subscription.destination());
            headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
            outbound.send(MessageBuilder.createMessage(payload, headers.getMessageHeaders()));
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not send a market quote WebSocket message", ex);
        }
    }

    private record SubscriptionId(String sessionId, String subscriptionId) {}
    private record Subscription(String userId, String destination, SocketIdentity web) {
        String owner() {
            if (web != null && web.visitor() != null) {
                return web.visitor().owner();
            }
            return SUBSCRIBED_DESTINATION.equals(destination) ? userId : null;
        }
    }
    private record Selection(List<String> codes, boolean reconnectRequired) {}
    public record WebQuoteMessage(String source, String fetchedAt, List<String> stockCodes, List<Quote> items,
            boolean reconnectRequired) {}
    private record IndexedSnapshot(String fetchedAt, Map<String, Quote> quotes) {}
    public record QuoteMessage(String source, String fetchedAt, List<String> stockCodes, List<Quote> items) {}
}
