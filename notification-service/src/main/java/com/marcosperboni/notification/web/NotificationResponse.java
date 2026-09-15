package com.marcosperboni.notification.web;

import com.marcosperboni.notification.domain.NotificationChannel;
import com.marcosperboni.notification.domain.NotificationStatus;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		UUID orderId,
		UUID customerId,
		NotificationChannel channel,
		String message,
		NotificationStatus status,
		Instant createdAt) {
}
