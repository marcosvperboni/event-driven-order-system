package com.marcosperboni.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 10)
	private NotificationChannel channel = NotificationChannel.EMAIL;

	@Column(name = "message", nullable = false)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 10)
	private NotificationStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	protected Notification() {
		// JPA
	}

	private Notification(UUID id, UUID orderId, UUID customerId, NotificationChannel channel, String message,
			NotificationStatus status, Instant createdAt) {
		this.id = id;
		this.orderId = Objects.requireNonNull(orderId, "orderId");
		this.customerId = Objects.requireNonNull(customerId, "customerId");
		this.channel = Objects.requireNonNull(channel, "channel");
		this.message = Objects.requireNonNull(message, "message");
		this.status = Objects.requireNonNull(status, "status");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
	}

	public static Notification create(UUID orderId, UUID customerId, NotificationChannel channel, String message,
			NotificationStatus status) {
		return new Notification(UUID.randomUUID(), orderId, customerId, channel, message, status, Instant.now());
	}

	public UUID getId() {
		return id;
	}

	public UUID getOrderId() {
		return orderId;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public NotificationChannel getChannel() {
		return channel;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = Objects.requireNonNull(message, "message");
	}

	public NotificationStatus getStatus() {
		return status;
	}

	public void setStatus(NotificationStatus status) {
		this.status = Objects.requireNonNull(status, "status");
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public long getVersion() {
		return version;
	}
}
