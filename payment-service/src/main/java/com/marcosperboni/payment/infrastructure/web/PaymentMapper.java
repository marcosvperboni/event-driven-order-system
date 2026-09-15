package com.marcosperboni.payment.infrastructure.web;

import com.marcosperboni.payment.domain.Payment;

public final class PaymentMapper {

	private PaymentMapper() {
	}

	public static PaymentResponse toResponse(Payment payment) {
		return new PaymentResponse(
				payment.getId(),
				payment.getOrderId(),
				payment.getCustomerId(),
				payment.getAmount(),
				payment.getStatus(),
				payment.getReason(),
				payment.getCreatedAt(),
				payment.getUpdatedAt());
	}
}
