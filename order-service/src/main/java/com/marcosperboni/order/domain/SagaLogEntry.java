package com.marcosperboni.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_log")
public class SagaLogEntry {

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "event_type", nullable = false, length = 150)
	private String eventType;

	@Column(columnDefinition = "text")
	private String payload;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected SagaLogEntry() {
	}

	private SagaLogEntry(UUID id, UUID orderId, String eventType, String payload, Instant createdAt) {
		this.id = id;
		this.orderId = orderId;
		this.eventType = eventType;
		this.payload = payload;
		this.createdAt = createdAt;
	}

	public static SagaLogEntry of(UUID orderId, String eventType, String payload) {
		return new SagaLogEntry(UUID.randomUUID(), orderId, eventType, payload, Instant.now());
	}

	public UUID getId() {
		return id;
	}

	public UUID getOrderId() {
		return orderId;
	}

	public String getEventType() {
		return eventType;
	}

	public String getPayload() {
		return payload;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
