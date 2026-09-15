package com.marcosperboni.payment.infrastructure.messaging.event;

import com.marcosperboni.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompensatedEvent(
		UUID eventId,
		UUID correlationId,
		UUID orderId,
		Instant occurredAt,
		UUID paymentId,
		PaymentStatus status) {
}
