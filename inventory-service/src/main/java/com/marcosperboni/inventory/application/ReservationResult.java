package com.marcosperboni.inventory.application;

import com.marcosperboni.inventory.infrastructure.messaging.event.ReservationStatus;

public record ReservationResult(ReservationStatus status, String reason) {

	public static ReservationResult reserved() {
		return new ReservationResult(ReservationStatus.RESERVED, null);
	}

	public static ReservationResult rejected(String reason) {
		return new ReservationResult(ReservationStatus.REJECTED, reason);
	}
}
