package com.marcosperboni.notification.web;

import com.marcosperboni.notification.domain.NotificationStatus;

public record NotificationUpdateRequest(NotificationStatus status, String message) {
}
