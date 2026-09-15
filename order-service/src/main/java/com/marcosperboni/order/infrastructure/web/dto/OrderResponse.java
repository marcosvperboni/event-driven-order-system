package com.marcosperboni.order.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
		UUID id,
		UUID customerId,
		String status,
		BigDecimal totalAmount,
		UUID paymentId,
		List<OrderItemResponse> items,
		Instant createdAt,
		Instant updatedAt) {
}
