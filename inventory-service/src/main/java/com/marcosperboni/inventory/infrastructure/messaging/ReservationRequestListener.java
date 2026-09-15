package com.marcosperboni.inventory.infrastructure.messaging;

import com.marcosperboni.inventory.application.ReservationResult;
import com.marcosperboni.inventory.application.ReservationService;
import com.marcosperboni.inventory.infrastructure.config.CorrelationIdFilter;
import com.marcosperboni.inventory.infrastructure.messaging.event.InventoryReservationProcessedEvent;
import com.marcosperboni.inventory.infrastructure.messaging.event.InventoryReservationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ReservationRequestListener {

	private static final Logger log = LoggerFactory.getLogger(ReservationRequestListener.class);
	private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

	private final ReservationService reservationService;
	private final ReservationEventPublisher publisher;
	private final StringRedisTemplate redisTemplate;

	public ReservationRequestListener(
			ReservationService reservationService, ReservationEventPublisher publisher, StringRedisTemplate redisTemplate) {
		this.reservationService = reservationService;
		this.publisher = publisher;
		this.redisTemplate = redisTemplate;
	}

	@KafkaListener(topics = KafkaTopics.RESERVATION_REQUESTED)
	public void onReservationRequested(InventoryReservationRequestedEvent event) {
		String idempotencyKey = "idempotency:inventory-service:" + event.getEventId();
		Boolean firstSeen = redisTemplate.opsForValue().setIfAbsent(idempotencyKey, "1", IDEMPOTENCY_TTL);
		if (Boolean.FALSE.equals(firstSeen)) {
			log.info("duplicate event {} already processed, skipping", event.getEventId());
			return;
		}

		MDC.put(CorrelationIdFilter.MDC_KEY, event.getCorrelationId().toString());
		try {
			ReservationResult result = reservationService.reserve(event.getItems());
			log.info("reservation for order {} resolved as {}", event.getOrderId(), result.status());
			publisher.publishProcessed(new InventoryReservationProcessedEvent(
					event.getCorrelationId(), event.getOrderId(), result.status(), result.reason()));
		} finally {
			MDC.remove(CorrelationIdFilter.MDC_KEY);
		}
	}
}
