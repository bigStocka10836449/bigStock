package com.bigstock.biz.component;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectInterceptor implements ChannelInterceptor {

    private final SocketSingleSessionService sessions;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        if (command == null) return message;

        // Closing must not renew a session or require that it is still current.
        if (command == StompCommand.DISCONNECT) return message;

        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();
        if (principal == null || principal.getName() == null
                || principal.getName().isBlank()
                || sessionId == null || sessionId.isBlank()) {
            log.warn("[WS] Reject unauthenticated STOMP command={}", command);
            return null;
        }

        String userId = principal.getName();
        try {
            if (command == StompCommand.CONNECT) {
                sessions.singleEnforce(userId, sessionId);
                return message;
            }

            // Before allowing client commands, require the currently bound session.
            // Never silently permit when Redis is unavailable.
            if (command == StompCommand.SUBSCRIBE
                    || command == StompCommand.UNSUBSCRIBE
                    || command == StompCommand.SEND) {
                if (!sessions.renewIfCurrent(userId, sessionId)) {
                    log.warn("[WS] Reject stale session command={}", command);
                    return null;
                }
            }
            return message;
        } catch (RuntimeException e) {
            log.warn("[WS] Session validation unavailable; command={}", command, e);
            return null;
        }
    }
}
