package com.marcosperboni.notification.infrastructure.persistence;

import com.marcosperboni.notification.domain.Notification;
import com.marcosperboni.notification.domain.NotificationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	Page<Notification> findByOrderId(UUID orderId, Pageable pageable);

	Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

	Page<Notification> findByOrderIdAndStatus(UUID orderId, NotificationStatus status, Pageable pageable);
}
