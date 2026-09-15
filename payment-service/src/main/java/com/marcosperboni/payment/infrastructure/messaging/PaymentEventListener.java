package com.marcosperboni.payment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcosperboni.payment.application.PaymentService;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentCompensationRequestedEvent;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class PaymentEventListener {

	private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);
	private static final String IDEMPOTENCY_PREFIX = "idempotency:payment-service:";
	private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

	private final PaymentService paymentService;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public PaymentEventListener(PaymentService paymentService, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.paymentService = paymentService;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = KafkaTopics.PAYMENT_REQUESTED)
	public void onPaymentRequested(String payload) throws Exception {
		PaymentRequestedEvent event = objectMapper.readValue(payload, PaymentRequestedEvent.class);
		if (!tryClaim(event.eventId())) {
			log.info("Duplicate payment.requested.events eventId={} skipped", event.eventId());
			return;
		}
		MDC.put("correlationId", event.correlationId().toString());
		try {
			paymentService.processPaymentRequested(event);
		} finally {
			MDC.remove("correlationId");
		}
	}

	@KafkaListener(topics = KafkaTopics.PAYMENT_COMPENSATION_REQUESTED)
	public void onCompensationRequested(String payload) throws Exception {
		PaymentCompensationRequestedEvent event = objectMapper.readValue(payload, PaymentCompensationRequestedEvent.class);
		if (!tryClaim(event.eventId())) {
			log.info("Duplicate payment.compensation.requested.events eventId={} skipped", event.eventId());
			return;
		}
		MDC.put("correlationId", event.correlationId().toString());
		try {
			paymentService.processCompensationRequested(event);
		} finally {
			MDC.remove("correlationId");
		}
	}

	private boolean tryClaim(UUID eventId) {
		Boolean claimed = redisTemplate.opsForValue().setIfAbsent(IDEMPOTENCY_PREFIX + eventId, "1", IDEMPOTENCY_TTL);
		return Boolean.TRUE.equals(claimed);
	}
}
