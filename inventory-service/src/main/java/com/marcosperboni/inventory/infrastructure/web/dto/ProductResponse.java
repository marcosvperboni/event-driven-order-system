package com.marcosperboni.inventory.infrastructure.web.dto;

import com.marcosperboni.inventory.domain.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
		UUID id,
		String sku,
		String name,
		int availableQuantity,
		BigDecimal unitPrice,
		Instant createdAt,
		Instant updatedAt) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getSku(),
				product.getName(),
				product.getAvailableQuantity(),
				product.getUnitPrice(),
				product.getCreatedAt(),
				product.getUpdatedAt());
	}
}
