package com.marcosperboni.order.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
		UUID id,
		UUID productId,
		String productName,
		int quantity,
		BigDecimal unitPrice) {
}
