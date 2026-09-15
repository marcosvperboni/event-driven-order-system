package com.marcosperboni.notification.web;

import com.marcosperboni.notification.domain.Notification;

final class NotificationMapper {

	private NotificationMapper() {
	}

	static NotificationResponse toResponse(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getOrderId(),
				notification.getCustomerId(),
				notification.getChannel(),
				notification.getMessage(),
				notification.getStatus(),
				notification.getCreatedAt());
	}
}
