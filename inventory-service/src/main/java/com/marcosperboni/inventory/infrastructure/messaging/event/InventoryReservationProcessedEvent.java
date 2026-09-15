package com.marcosperboni.inventory.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors the {@code inventory.reservation.processed.events} contract published by inventory-service.
 */
public class InventoryReservationProcessedEvent {

	private UUID eventId;
	private UUID correlationId;
	private UUID orderId;
	private Instant occurredAt;
	private ReservationStatus status;
	private String reason;

	public InventoryReservationProcessedEvent() {
	}

	public InventoryReservationProcessedEvent(UUID correlationId, UUID orderId, ReservationStatus status, String reason) {
		this.eventId = UUID.randomUUID();
		this.correlationId = correlationId;
		this.orderId = orderId;
		this.occurredAt = Instant.now();
		this.status = status;
		this.reason = reason;
	}

	public UUID getEventId() {
		return eventId;
	}

	public void setEventId(UUID eventId) {
		this.eventId = eventId;
	}

	public UUID getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(UUID correlationId) {
		this.correlationId = correlationId;
	}

	public UUID getOrderId() {
		return orderId;
	}

	public void setOrderId(UUID orderId) {
		this.orderId = orderId;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public ReservationStatus getStatus() {
		return status;
	}

	public void setStatus(ReservationStatus status) {
		this.status = status;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
