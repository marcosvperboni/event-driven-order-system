package com.marcosperboni.inventory.infrastructure.messaging.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors the {@code inventory.reservation.requested.events} contract published by order-service.
 */
public class InventoryReservationRequestedEvent {

	private UUID eventId;
	private UUID correlationId;
	private UUID orderId;
	private Instant occurredAt;
	private List<ReservationItem> items;

	public InventoryReservationRequestedEvent() {
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

	public List<ReservationItem> getItems() {
		return items;
	}

	public void setItems(List<ReservationItem> items) {
		this.items = items;
	}
}
