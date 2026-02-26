package com.bigstock.biz.component;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalSessionRegistry {

	private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	public void register(WebSocketSession session) {
		sessions.put(session.getId(), session);
	}

	public void remove(String sessionId) {
		sessions.remove(sessionId);
	}

	/**
	 * 只關閉存在於本地的session的socket連線，關閉時會一併刪除在redis的資料
	 * @param sessionId
	 */
	public void close(String sessionId) {
		WebSocketSession s = sessions.get(sessionId);
		if (s != null && s.isOpen()) {
			try {
				s.close();
			} catch (IOException ignored) {
			}
			sessions.remove(sessionId);
		}
	}
}
