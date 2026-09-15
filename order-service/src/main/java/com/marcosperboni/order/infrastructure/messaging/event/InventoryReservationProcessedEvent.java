package com.marcosperboni.order.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservationProcessedEvent(
		UUID eventId,
		UUID correlationId,
		UUID orderId,
		Instant occurredAt,
		String status,
		String reason) {
}
