package com.marcosperboni.payment.infrastructure.messaging;

public final class KafkaTopics {

	public static final String PAYMENT_REQUESTED = "payment.requested.events";
	public static final String PAYMENT_PROCESSED = "payment.processed.events";
	public static final String PAYMENT_COMPENSATION_REQUESTED = "payment.compensation.requested.events";
	public static final String PAYMENT_COMPENSATED = "payment.compensated.events";

	private KafkaTopics() {
	}
}
