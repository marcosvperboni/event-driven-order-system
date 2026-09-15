package com.marcosperboni.order.infrastructure.messaging;

public final class KafkaTopics {

	public static final String ORDER_CREATED = "order.created.events";
	public static final String PAYMENT_REQUESTED = "payment.requested.events";
	public static final String PAYMENT_PROCESSED = "payment.processed.events";
	public static final String INVENTORY_RESERVATION_REQUESTED = "inventory.reservation.requested.events";
	public static final String INVENTORY_RESERVATION_PROCESSED = "inventory.reservation.processed.events";
	public static final String PAYMENT_COMPENSATION_REQUESTED = "payment.compensation.requested.events";
	public static final String PAYMENT_COMPENSATED = "payment.compensated.events";
	public static final String ORDER_STATUS = "order.status.events";

	private KafkaTopics() {
	}
}
