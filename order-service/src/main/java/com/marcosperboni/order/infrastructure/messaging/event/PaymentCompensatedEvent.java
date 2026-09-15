package com.marcosperboni.order.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompensatedEvent(
		UUID eventId,
		UUID correlationId,
		UUID orderId,
		Instant occurredAt,
		UUID paymentId,
		String status) {
}
