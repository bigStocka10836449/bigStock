package com.bigstock.biz.component;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Component
public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {

	/**
	 * 從WebSocket的Header中取出會需要使用的UserId，做sessionId的統計
	 */
    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                     WebSocketHandler wsHandler,
                                     Map<String, Object> attributes) {

        String userId = (String) attributes.get("userId");
        return () -> userId;
    }
}
