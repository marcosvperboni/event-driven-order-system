package com.marcosperboni.notification.web;

import com.marcosperboni.notification.domain.NotificationChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NotificationRequest(
		@NotNull UUID orderId,
		@NotNull UUID customerId,
		@NotNull NotificationChannel channel,
		@NotBlank String message) {
}
