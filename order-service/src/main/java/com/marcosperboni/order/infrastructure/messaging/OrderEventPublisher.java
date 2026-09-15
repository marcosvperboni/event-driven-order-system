package com.marcosperboni.order.infrastructure.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OrderEventPublisher {

	private static final String CORRELATION_HEADER = "X-Correlation-Id";

	private final KafkaTemplate<Object, Object> kafkaTemplate;
	private final SagaLogWriter sagaLogWriter;

	public OrderEventPublisher(KafkaTemplate<Object, Object> kafkaTemplate, SagaLogWriter sagaLogWriter) {
		this.kafkaTemplate = kafkaTemplate;
		this.sagaLogWriter = sagaLogWriter;
	}

	public void publish(String topic, UUID orderId, Object event) {
		ProducerRecord<Object, Object> record = new ProducerRecord<>(topic, orderId.toString(), event);
		record.headers().add(CORRELATION_HEADER, orderId.toString().getBytes(StandardCharsets.UTF_8));
		kafkaTemplate.send(record);
		sagaLogWriter.log(orderId, topic, event);
	}
}
