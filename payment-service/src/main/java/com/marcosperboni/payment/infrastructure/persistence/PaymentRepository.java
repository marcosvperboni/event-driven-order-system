package com.marcosperboni.payment.infrastructure.persistence;

import com.marcosperboni.payment.domain.Payment;
import com.marcosperboni.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

	Page<Payment> findByOrderId(UUID orderId, Pageable pageable);

	Page<Payment> findByStatusAndOrderId(PaymentStatus status, UUID orderId, Pageable pageable);
}
