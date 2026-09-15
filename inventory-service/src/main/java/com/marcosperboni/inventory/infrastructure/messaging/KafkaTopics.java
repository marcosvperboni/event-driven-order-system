package com.marcosperboni.inventory.infrastructure.messaging;

public final class KafkaTopics {

	public static final String RESERVATION_REQUESTED = "inventory.reservation.requested.events";
	public static final String RESERVATION_PROCESSED = "inventory.reservation.processed.events";

	private KafkaTopics() {
	}
}
