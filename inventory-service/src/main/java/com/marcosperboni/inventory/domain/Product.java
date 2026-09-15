package com.marcosperboni.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String sku;

	@Column(nullable = false)
	private String name;

	@Min(0)
	@Column(name = "available_quantity", nullable = false)
	private int availableQuantity;

	@Column(name = "unit_price", nullable = false)
	private BigDecimal unitPrice;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected Product() {
	}

	public Product(UUID id, String sku, String name, int availableQuantity, BigDecimal unitPrice) {
		this.id = id;
		this.sku = sku;
		this.name = name;
		this.availableQuantity = availableQuantity;
		this.unitPrice = unitPrice;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	/**
	 * Decrements available stock. Throws if requested quantity exceeds what's available,
	 * enforcing the {@code >= 0} invariant explicitly (in addition to the {@code @Min(0)} annotation).
	 */
	public void decrementStock(int quantity) {
		if (quantity > this.availableQuantity) {
			throw new IllegalStateException("insufficient stock for product " + id);
		}
		this.availableQuantity -= quantity;
	}

	public boolean hasEnoughStock(int quantity) {
		return this.availableQuantity >= quantity;
	}

	public UUID getId() {
		return id;
	}

	public String getSku() {
		return sku;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAvailableQuantity() {
		return availableQuantity;
	}

	public void setAvailableQuantity(int availableQuantity) {
		this.availableQuantity = availableQuantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public long getVersion() {
		return version;
	}
}
