package com.bigstock.biz.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bigstock.biz.service.MarketQuoteEvents;
import com.bigstock.biz.service.MarketQuoteSubscriptions;
import com.bigstock.biz.service.MarketQuoteWebSocketService;
import com.bigstock.biz.service.MarketQuotesService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MarketQuoteSubscriptionController {

    private final MarketQuotesService quotes;
    private final MarketQuoteSubscriptions subscriptions;
    private final MarketQuoteEvents events;

    @PostMapping("/biz/marketQuotes/subscriptions")
    public SubscriptionResponse change(JwtAuthenticationToken auth, @RequestBody ChangeRequest request)
            throws IOException {
        String owner = subject(auth);
        if (request.userId() == null || request.userId().isBlank()) {
            return new SubscriptionResponse(MarketQuoteWebSocketService.DEFAULT_DESTINATION,
                    MarketQuoteSubscriptions.DEFAULT_STOCKS);
        }
        requireOwner(owner, request.userId());
        if (request.stock() == null || request.stock().isBlank() || request.subscribed() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected stock and subscribed");
        }

        String stock = request.stock().strip();
        List<String> existing = subscriptions.stockCodes(owner);
        // Allow a stored code to be removed even if the quote vanished from the latest feed.
        String code = !request.subscribed() && existing.contains(stock) ? stock : quotes.resolveStockCode(stock);
        if (subscriptions.change(owner, code, request.subscribed())) {
            events.subscriptionsUpdated(owner);
        }
        return new SubscriptionResponse(MarketQuoteWebSocketService.SUBSCRIBED_DESTINATION,
                subscriptions.stockCodes(owner));
    }

    @PostMapping("/biz/marketQuotes/subscriptions/refresh")
    public RefreshResponse refresh(JwtAuthenticationToken auth, @RequestBody RefreshRequest request) {
        String owner = subject(auth);
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected userId");
        }
        requireOwner(owner, request.userId());
        if (!subscriptions.refresh(owner)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription list");
        }
        return new RefreshResponse(MarketQuoteSubscriptions.LIFETIME.toSeconds());
    }

    private String subject(JwtAuthenticationToken auth) {
        if (auth == null || auth.getToken().getSubject() == null || auth.getToken().getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT subject");
        }
        return auth.getToken().getSubject();
    }

    private void requireOwner(String owner, String requestedUserId) {
        if (!owner.equals(requestedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Subscription belongs to another user");
        }
    }

    public record ChangeRequest(String stock, Boolean subscribed, String userId) {}
    public record RefreshRequest(String userId) {}
    public record SubscriptionResponse(String destination, List<String> stockCodes) {}
    public record RefreshResponse(long ttlSeconds) {}
}
