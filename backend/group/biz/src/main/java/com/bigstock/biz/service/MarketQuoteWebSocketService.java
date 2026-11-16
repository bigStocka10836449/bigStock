package com.bigstock.biz.service;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final MarketQuotesService quotes;
    private final MarketQuoteSubscriptions subscriptions;
    private final LocalSessionRegistry sessions;
    private final ObjectMapper mapper;
    private final MessageChannel outbound;
    private final Map<SubscriptionId, Subscription> listeners = new ConcurrentHashMap<>();

    public MarketQuoteWebSocketService(MarketQuotesService quotes, MarketQuoteSubscriptions subscriptions,
            LocalSessionRegistry sessions, ObjectMapper mapper,
            @Qualifier("clientOutboundChannel") MessageChannel outbound) {
        this.quotes = quotes;
        this.subscriptions = subscriptions;
        this.sessions = sessions;
        this.mapper = mapper;
        this.outbound = outbound;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String destination = headers.getDestination();
        if (!DEFAULT_DESTINATION.equals(destination) && !SUBSCRIBED_DESTINATION.equals(destination)) {
            return;
        }

        Principal user = event.getUser();
        String sessionId = headers.getSessionId();
        String subscriptionId = headers.getSubscriptionId();
        if (user == null || sessionId == null || subscriptionId == null) {
            return;
        }
        var id = new SubscriptionId(sessionId, subscriptionId);
        var subscription = new Subscription(user.getName(), destination);
        listeners.put(id, subscription);
        if (!sessions.isOpen(sessionId)) {
            listeners.remove(id, subscription);
            return;
        }

        try {
            List<String> codes = subscription.personal() ? subscriptions.stockCodes(subscription.userId())
                    : MarketQuoteSubscriptions.DEFAULT_STOCKS;
            for (String source : MarketQuotesService.SOURCES) {
                send(id, subscription, source, index(quotes.readSnapshot(source)), codes);
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
            List<String> codes = subscription.personal()
                    ? codesByUser.computeIfAbsent(subscription.userId(), subscriptions::stockCodes)
                    : MarketQuoteSubscriptions.DEFAULT_STOCKS;
            send(id, subscription, source, indexed, codes);
        });
    }

    private void pushSubscriptionsChanged(String userId) throws IOException {
        var targets = listeners.entrySet().stream()
                .filter(entry -> entry.getValue().personal() && userId.equals(entry.getValue().userId()))
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        List<String> codes = subscriptions.stockCodes(userId);
        for (String source : MarketQuotesService.SOURCES) {
            IndexedSnapshot snapshot = index(quotes.readSnapshot(source));
            targets.forEach(entry -> send(entry.getKey(), entry.getValue(), source, snapshot, codes));
        }
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
            List<String> codes) {
        if (listeners.get(id) != subscription || !sessions.isOpen(id.sessionId())) {
            return;
        }
        List<Quote> items = codes.stream().filter(snapshot.quotes()::containsKey)
                .map(snapshot.quotes()::get).toList();
        try {
            byte[] payload = mapper.writeValueAsBytes(new QuoteMessage(source, snapshot.fetchedAt(), codes, items));
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
    private record Subscription(String userId, String destination) {
        boolean personal() {
            return SUBSCRIBED_DESTINATION.equals(destination);
        }
    }
    private record IndexedSnapshot(String fetchedAt, Map<String, Quote> quotes) {}
    public record QuoteMessage(String source, String fetchedAt, List<String> stockCodes, List<Quote> items) {}
}
