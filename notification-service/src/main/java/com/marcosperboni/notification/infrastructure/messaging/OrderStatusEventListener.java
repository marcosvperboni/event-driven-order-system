package com.marcosperboni.notification.infrastructure.messaging;

import com.marcosperboni.notification.application.NotificationService;
import com.marcosperboni.notification.domain.NotificationChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OrderStatusEventListener {

	private static final Logger log = LoggerFactory.getLogger(OrderStatusEventListener.class);
	private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:notification-service:";

	private final NotificationService notificationService;
	private final StringRedisTemplate redisTemplate;

	public OrderStatusEventListener(NotificationService notificationService, StringRedisTemplate redisTemplate) {
		this.notificationService = notificationService;
		this.redisTemplate = redisTemplate;
	}

	@KafkaListener(topics = "order.status.events")
	public void onOrderStatus(OrderStatusEvent event) {
		MDC.put("correlationId", event.correlationId().toString());
		try {
			String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + event.eventId();
			boolean firstSeen = Boolean.TRUE.equals(
					redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", Duration.ofHours(24)));
			if (!firstSeen) {
				log.info("Skipping duplicate order.status.events event {} for order {}", event.eventId(),
						event.orderId());
				return;
			}

			String message = buildMessage(event);
			notificationService.send(event.orderId(), event.customerId(), NotificationChannel.EMAIL, message);
			log.info("Notification sent for order {}: {}", event.orderId(), message);
		} finally {
			MDC.remove("correlationId");
		}
	}

	private String buildMessage(OrderStatusEvent event) {
		return switch (event.status()) {
			case CONFIRMED -> "Your order " + event.orderId() + " has been confirmed!";
			case CANCELLED -> "Your order " + event.orderId() + " was cancelled: " + event.reason();
		};
	}
}
