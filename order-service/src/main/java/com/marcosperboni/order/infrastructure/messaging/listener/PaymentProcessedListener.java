package com.marcosperboni.order.infrastructure.messaging.listener;

import com.marcosperboni.order.application.OrderSagaService;
import com.marcosperboni.order.infrastructure.messaging.IdempotencyService;
import com.marcosperboni.order.infrastructure.messaging.KafkaTopics;
import com.marcosperboni.order.infrastructure.messaging.SagaLogWriter;
import com.marcosperboni.order.infrastructure.messaging.event.PaymentProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessedListener {

	private static final Logger log = LoggerFactory.getLogger(PaymentProcessedListener.class);

	private final IdempotencyService idempotencyService;
	private final SagaLogWriter sagaLogWriter;
	private final OrderSagaService sagaService;

	public PaymentProcessedListener(IdempotencyService idempotencyService, SagaLogWriter sagaLogWriter,
			OrderSagaService sagaService) {
		this.idempotencyService = idempotencyService;
		this.sagaLogWriter = sagaLogWriter;
		this.sagaService = sagaService;
	}

	@KafkaListener(topics = KafkaTopics.PAYMENT_PROCESSED, properties =
			"spring.json.value.default.type=com.marcosperboni.order.infrastructure.messaging.event.PaymentProcessedEvent")
	public void onMessage(PaymentProcessedEvent event) {
		if (!idempotencyService.isNewEvent(event.eventId())) {
			log.info("Duplicate event {} on {} skipped", event.eventId(), KafkaTopics.PAYMENT_PROCESSED);
			return;
		}
		MDC.put("correlationId", event.correlationId().toString());
		try {
			sagaLogWriter.log(event.orderId(), KafkaTopics.PAYMENT_PROCESSED, event);
			sagaService.handlePaymentProcessed(event);
		} finally {
			MDC.remove("correlationId");
		}
	}
}
