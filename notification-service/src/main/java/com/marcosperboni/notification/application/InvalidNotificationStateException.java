package com.marcosperboni.notification.application;

public class InvalidNotificationStateException extends RuntimeException {

	public InvalidNotificationStateException(String message) {
		super(message);
	}
}
