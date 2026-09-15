package com.marcosperboni.order.infrastructure.messaging.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InventoryReservationRequestedEvent(
		UUID eventId,
		UUID correlationId,
		UUID orderId,
		Instant occurredAt,
		List<Item> items) {

	public record Item(UUID productId, int quantity) {
	}
}
