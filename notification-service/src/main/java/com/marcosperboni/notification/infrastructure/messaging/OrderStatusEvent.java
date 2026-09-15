package com.marcosperboni.notification.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors the {@code order.status.events} contract published by order-service.
 * Deliberately not shared as a library (anti-corruption layer).
 */
public record OrderStatusEvent(
		UUID eventId,
		UUID correlationId,
		UUID orderId,
		Instant occurredAt,
		UUID customerId,
		OrderStatus status,
		String reason) {

	public enum OrderStatus {
		CONFIRMED,
		CANCELLED
	}
}
