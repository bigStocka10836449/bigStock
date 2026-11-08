package com.bigstock.biz.component;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** One active STOMP session per principal, across Spring Boot instances. */
@Service
@RequiredArgsConstructor
public class SocketSingleSessionService {

	private static final long TTL_SECONDS = Duration.ofMinutes(30).getSeconds();

	// Atomically replace the principal's active session, register its reverse
	// mapping,
	// and publish the old session ID to all instances. Previous session cannot
	// renew
	// either key because renew() validates both mappings before changing TTL.
	private static final DefaultRedisScript<Long> CONNECT_SCRIPT = script("""
			local old = redis.call('GET', KEYS[1])
			redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
			redis.call('SET', KEYS[2], ARGV[3], 'EX', ARGV[2])
			if old and old ~= ARGV[1] then
			    redis.call('PUBLISH', 'WS_KICK', old)
			end
			return 1
			""");

	// A two-way comparison prevents an obsolete session from extending the new
	// session's TTL, and avoids the GET/EXPIRE time-of-check/time-of-use race.
	private static final DefaultRedisScript<Long> RENEW_SCRIPT = script("""
			if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
			if redis.call('GET', KEYS[2]) ~= ARGV[2] then return 0 end
			redis.call('EXPIRE', KEYS[1], ARGV[3])
			redis.call('EXPIRE', KEYS[2], ARGV[3])
			return 1
			""");

	// Only remove the forward mapping when it still names the disconnecting
	// session. Never delete a newer session's mapping from an old DISCONNECT.
	private static final DefaultRedisScript<Long> DISCONNECT_SCRIPT = script("""
			local user = redis.call('GET', KEYS[2])
			if not user then return 0 end
			if redis.call('GET', KEYS[1]) == ARGV[1] then
			    redis.call('DEL', KEYS[1])
			end
			redis.call('DEL', KEYS[2])
			return 1
			""");

	private final StringRedisTemplate redis;

	private static DefaultRedisScript<Long> script(String lua) {
		DefaultRedisScript<Long> result = new DefaultRedisScript<>();
		result.setScriptText(lua);
		result.setResultType(Long.class);
		return result;
	}

	private static String userKey(String userId) {
		return "ws:user:" + userId;
	}

	private static String sessionKey(String sessionId) {
		return "ws:session:" + sessionId;
	}

	public void singleEnforce(String userId, String sessionId) {
		requireId(userId);
		requireId(sessionId);
		Long result = redis.execute(CONNECT_SCRIPT, List.of(userKey(userId), sessionKey(sessionId)), sessionId,
				String.valueOf(TTL_SECONDS), userId);
		if (!Long.valueOf(1L).equals(result)) {
			throw new IllegalStateException("Cannot register WebSocket session");
		}
	}

	public boolean renewIfCurrent(String userId, String sessionId) {
		requireId(userId);
		requireId(sessionId);
		Long result = redis.execute(RENEW_SCRIPT, List.of(userKey(userId), sessionKey(sessionId)), sessionId, userId,
				String.valueOf(TTL_SECONDS));
		return Long.valueOf(1L).equals(result);
	}

	public void disconnect(String sessionId) {
		if (sessionId == null || sessionId.isBlank())
			return;
		// Reverse key identifies the principal so callers need only sessionId.
		String userId = redis.opsForValue().get(sessionKey(sessionId));
		if (userId == null || userId.isBlank())
			return;
		redis.execute(DISCONNECT_SCRIPT, List.of(userKey(userId), sessionKey(sessionId)), sessionId);
	}

	private static void requireId(String value) {
		if (value == null || value.isBlank() || value.length() > 256) {
			throw new IllegalArgumentException("Invalid WebSocket identity/session");
		}
	}
}
