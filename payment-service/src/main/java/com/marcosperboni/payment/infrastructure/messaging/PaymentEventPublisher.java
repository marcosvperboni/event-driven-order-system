package com.marcosperboni.payment.infrastructure.messaging;

import com.marcosperboni.payment.infrastructure.messaging.event.PaymentCompensatedEvent;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentProcessedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class PaymentEventPublisher {

	public static final String CORRELATION_HEADER = "X-Correlation-Id";

	private final KafkaTemplate<Object, Object> kafkaTemplate;

	public PaymentEventPublisher(KafkaTemplate<Object, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void publishProcessed(PaymentProcessedEvent event) {
		send(KafkaTopics.PAYMENT_PROCESSED, event.orderId().toString(), event.correlationId().toString(), event);
	}

	public void publishCompensated(PaymentCompensatedEvent event) {
		send(KafkaTopics.PAYMENT_COMPENSATED, event.orderId().toString(), event.correlationId().toString(), event);
	}

	private void send(String topic, String key, String correlationId, Object event) {
		ProducerRecord<Object, Object> record = new ProducerRecord<>(topic, key, event);
		record.headers().add(CORRELATION_HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
		kafkaTemplate.send(record);
	}
}
