package com.marcosperboni.order.infrastructure.messaging.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
		UUID eventId,
		UUID correlationId,
		UUID orderId,
		Instant occurredAt,
		UUID customerId,
		List<Item> items,
		BigDecimal totalAmount) {

	public record Item(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
	}
}
