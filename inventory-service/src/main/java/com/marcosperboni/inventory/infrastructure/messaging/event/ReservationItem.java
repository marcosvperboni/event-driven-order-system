package com.marcosperboni.inventory.infrastructure.messaging.event;

import java.util.UUID;

public class ReservationItem {

	private UUID productId;
	private int quantity;

	public ReservationItem() {
	}

	public ReservationItem(UUID productId, int quantity) {
		this.productId = productId;
		this.quantity = quantity;
	}

	public UUID getProductId() {
		return productId;
	}

	public void setProductId(UUID productId) {
		this.productId = productId;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
