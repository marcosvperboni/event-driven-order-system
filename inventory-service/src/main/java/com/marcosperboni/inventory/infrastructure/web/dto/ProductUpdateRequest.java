package com.marcosperboni.inventory.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductUpdateRequest(
		@NotBlank String name,
		@Min(0) int availableQuantity,
		@NotNull @DecimalMin("0.0") BigDecimal unitPrice) {
}
