package com.marcosperboni.order.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record SagaLogEntryResponse(
		UUID id,
		UUID orderId,
		String eventType,
		String payload,
		Instant createdAt) {
}
