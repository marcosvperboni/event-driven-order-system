package com.marcosperboni.payment.infrastructure.web;

import com.marcosperboni.payment.domain.PaymentStatus;

public record PaymentUpdateRequest(PaymentStatus status, String reason) {
}
