package com.marcosperboni.order.infrastructure.messaging;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class IdempotencyService {

	private static final String KEY_PREFIX = "idempotency:order-service:";
	private static final Duration TTL = Duration.ofHours(24);

	private final StringRedisTemplate redisTemplate;

	public IdempotencyService(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * @return true if this is the first time this eventId is seen (should be processed).
	 */
	public boolean isNewEvent(UUID eventId) {
		Boolean firstSeen = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + eventId, "1", TTL);
		return Boolean.TRUE.equals(firstSeen);
	}
}
