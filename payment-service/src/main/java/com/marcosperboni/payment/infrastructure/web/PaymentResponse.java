package com.marcosperboni.payment.infrastructure.web;

import com.marcosperboni.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
		UUID id,
		UUID orderId,
		UUID customerId,
		BigDecimal amount,
		PaymentStatus status,
		String reason,
		Instant createdAt,
		Instant updatedAt) {
}
