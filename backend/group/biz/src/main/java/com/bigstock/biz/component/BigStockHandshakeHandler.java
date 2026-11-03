package com.bigstock.biz.component;


import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Component
public class BigStockHandshakeHandler
        extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {

        Object value = attributes.get(
                WebSocketAttributes.USER_ID
        );

        if (!(value instanceof String userId)
                || userId.isBlank()) {

            return null;
        }

        return () -> userId;
    }
}
