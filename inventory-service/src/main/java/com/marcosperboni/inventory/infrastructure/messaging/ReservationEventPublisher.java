package com.marcosperboni.inventory.infrastructure.messaging;

import com.marcosperboni.inventory.infrastructure.config.CorrelationIdFilter;
import com.marcosperboni.inventory.infrastructure.messaging.event.InventoryReservationProcessedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class ReservationEventPublisher {

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public ReservationEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void publishProcessed(InventoryReservationProcessedEvent event) {
		ProducerRecord<String, Object> record = new ProducerRecord<>(
				KafkaTopics.RESERVATION_PROCESSED, event.getOrderId().toString(), event);
		record.headers().add(new RecordHeader(
				CorrelationIdFilter.HEADER, event.getCorrelationId().toString().getBytes(StandardCharsets.UTF_8)));
		kafkaTemplate.send(record);
	}
}
