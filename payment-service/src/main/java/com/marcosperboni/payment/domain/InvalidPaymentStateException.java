package com.marcosperboni.payment.domain;

public class InvalidPaymentStateException extends RuntimeException {

	public InvalidPaymentStateException(String message) {
		super(message);
	}
}
