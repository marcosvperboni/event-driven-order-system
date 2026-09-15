package com.marcosperboni.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrderStatus status;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "payment_id")
	private UUID paymentId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	protected Order() {
	}

	public Order(UUID id, UUID customerId, OrderStatus status, BigDecimal totalAmount, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.customerId = customerId;
		this.status = status;
		this.totalAmount = totalAmount;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void addItem(OrderItem item) {
		item.setOrder(this);
		items.add(item);
	}

	public void replaceItems(List<OrderItem> newItems) {
		items.clear();
		newItems.forEach(this::addItem);
	}

	public UUID getId() {
		return id;
	}

	public UUID getCustomerId() {
		return customerId;
	}

	public void setCustomerId(UUID customerId) {
		this.customerId = customerId;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(BigDecimal totalAmount) {
		this.totalAmount = totalAmount;
	}

	public UUID getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(UUID paymentId) {
		this.paymentId = paymentId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public long getVersion() {
		return version;
	}

	public List<OrderItem> getItems() {
		return items;
	}
}
