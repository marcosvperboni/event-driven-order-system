package com.marcosperboni.notification.application;

import com.marcosperboni.notification.domain.Notification;
import com.marcosperboni.notification.domain.NotificationChannel;
import com.marcosperboni.notification.domain.NotificationStatus;
import com.marcosperboni.notification.infrastructure.persistence.NotificationRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

	private final NotificationRepository repository;

	public NotificationService(NotificationRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public Notification send(UUID orderId, UUID customerId, NotificationChannel channel, String message) {
		Notification notification = Notification.create(orderId, customerId, channel, message, NotificationStatus.SENT);
		return repository.save(notification);
	}

	@Transactional(readOnly = true)
	public Page<Notification> list(UUID orderId, NotificationStatus status, Pageable pageable) {
		if (orderId != null && status != null) {
			return repository.findByOrderIdAndStatus(orderId, status, pageable);
		}
		if (orderId != null) {
			return repository.findByOrderId(orderId, pageable);
		}
		if (status != null) {
			return repository.findByStatus(status, pageable);
		}
		return repository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public Notification getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
	}

	@Transactional
	public Notification update(UUID id, NotificationStatus newStatus, String newMessage) {
		Notification notification = getById(id);
		if (newStatus != null) {
			if (notification.getStatus() != NotificationStatus.PENDING
					|| (newStatus != NotificationStatus.SENT && newStatus != NotificationStatus.FAILED)) {
				throw new InvalidNotificationStateException(
						"Illegal status transition: " + notification.getStatus() + " -> " + newStatus);
			}
			notification.setStatus(newStatus);
		}
		if (newMessage != null) {
			notification.setMessage(newMessage);
		}
		return notification;
	}

	@Transactional
	public void delete(UUID id) {
		if (!repository.existsById(id)) {
			throw new NotificationNotFoundException(id);
		}
		repository.deleteById(id);
	}
}
