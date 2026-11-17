package com.bigstock.biz.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.bigstock.biz.service.MarketQuoteEvents;
import com.bigstock.biz.service.MarketQuoteSubscriptions;
import com.bigstock.biz.service.MarketQuoteWebSocketService;
import com.bigstock.biz.service.MarketQuotesService;
import com.bigstock.biz.service.WebQuoteVisitor;
import com.bigstock.biz.service.WebQuoteVisitor.Visitor;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MarketQuoteSubscriptionController {

    private final MarketQuotesService quotes;
    private final MarketQuoteSubscriptions subscriptions;
    private final MarketQuoteEvents events;
    private final WebQuoteVisitor visitors;

    @PostMapping("/biz/marketQuotes/subscriptions")
    public SubscriptionResponse change(JwtAuthenticationToken auth, @RequestBody ChangeRequest request)
            throws IOException {
        String owner = subject(auth);
        if (request.userId() == null || request.userId().isBlank()) {
            return new SubscriptionResponse(MarketQuoteWebSocketService.DEFAULT_DESTINATION,
                    MarketQuoteSubscriptions.DEFAULT_STOCKS);
        }
        requireOwner(owner, request.userId());
        String code = resolveStockCode(owner, request.stock(), request.subscribed(), false);
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

    @GetMapping("/biz/marketQuotes/subscriptions/web")
    public ResponseEntity<WebListResponse> webList(JwtAuthenticationToken auth,
            @CookieValue(name = WebQuoteVisitor.COOKIE_NAME, required = false) String token) {
        requireWebGuest(auth);
        Visitor visitor = visitors.find(token);
        List<String> stockCodes = visitor == null ? List.of() : subscriptions.stockCodes(visitor.owner());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(webResponse(stockCodes, false));
    }

    @PostMapping("/biz/marketQuotes/subscriptions/web")
    public ResponseEntity<WebListResponse> changeWeb(JwtAuthenticationToken auth,
            @CookieValue(name = WebQuoteVisitor.COOKIE_NAME, required = false) String token,
            @RequestBody WebChangeRequest request) throws IOException {
        requireWebGuest(auth);
        Visitor visitor = visitors.find(token);
        String owner = visitor == null ? null : visitor.owner();
        String code = resolveStockCode(owner, request.stock(), request.subscribed(), true);
        var response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        boolean visitorCreated = visitor == null;
        if (visitorCreated) {
            if (!request.subscribed()) {
                return response.body(webResponse(List.of(), false));
            }
            var issued = visitors.issue();
            visitor = issued.visitor();
            response.header(HttpHeaders.SET_COOKIE, issued.cookie().toString());
        }
        if (subscriptions.changeWeb(visitor, code, request.subscribed())) {
            events.subscriptionsUpdated(visitor.owner());
        }
        return response.body(webResponse(subscriptions.stockCodes(visitor.owner()), visitorCreated));
    }

    @PostMapping("/biz/marketQuotes/subscriptions/web/refresh")
    public ResponseEntity<RefreshResponse> refreshWeb(JwtAuthenticationToken auth,
            @CookieValue(name = WebQuoteVisitor.COOKIE_NAME, required = false) String token) {
        requireWebGuest(auth);
        Visitor visitor = visitors.find(token);
        if (visitor == null || !subscriptions.refreshWeb(visitor)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No web subscription list");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, visitors.cookie(token).toString())
                .body(new RefreshResponse(MarketQuoteSubscriptions.LIFETIME.toSeconds()));
    }

    private WebListResponse webResponse(List<String> stockCodes, boolean visitorCreated) {
        return new WebListResponse(stockCodes, MarketQuoteWebSocketService.WEB_DESTINATION, visitorCreated);
    }

    private String resolveStockCode(String owner, String stock, Boolean subscribed, boolean codeOnly)
            throws IOException {
        if (stock == null || stock.isBlank() || subscribed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected stock and subscribed");
        }
        String value = stock.strip();
        // Stored codes can be removed when the stock is missing from the current feed.
        if (!subscribed && owner != null && subscriptions.stockCodes(owner).contains(value)) {
            return value;
        }
        return codeOnly ? quotes.requireStockCode(value) : quotes.resolveStockCode(value);
    }

    private void requireWebGuest(JwtAuthenticationToken auth) {
        subject(auth);
        if (!"web_guest".equals(auth.getToken().getClaimAsString("token_type"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Expected a web guest session");
        }
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
    public record WebChangeRequest(String stock, Boolean subscribed) {}
    public record WebListResponse(List<String> stockCodes, String destination, boolean visitorCreated) {}
    public record RefreshRequest(String userId) {}
    public record SubscriptionResponse(String destination, List<String> stockCodes) {}
    public record RefreshResponse(long ttlSeconds) {}
}
