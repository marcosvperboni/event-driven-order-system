package com.marcosperboni.payment.application;

import com.marcosperboni.payment.domain.InvalidPaymentStateException;
import com.marcosperboni.payment.domain.Payment;
import com.marcosperboni.payment.domain.PaymentNotFoundException;
import com.marcosperboni.payment.domain.PaymentStatus;
import com.marcosperboni.payment.infrastructure.messaging.PaymentEventPublisher;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentCompensatedEvent;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentCompensationRequestedEvent;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentProcessedEvent;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentRequestedEvent;
import com.marcosperboni.payment.infrastructure.persistence.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentService {

	private static final BigDecimal AUTHORIZATION_LIMIT = new BigDecimal("5000.00");
	private static final String REJECTION_REASON = "amount exceeds authorization limit";

	private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(PaymentStatus.class);

	static {
		ALLOWED_TRANSITIONS.put(PaymentStatus.PENDING, EnumSet.of(PaymentStatus.APPROVED, PaymentStatus.REJECTED));
		ALLOWED_TRANSITIONS.put(PaymentStatus.APPROVED, EnumSet.of(PaymentStatus.REFUNDED));
	}

	private final PaymentRepository repository;
	private final PaymentEventPublisher eventPublisher;

	public PaymentService(PaymentRepository repository, PaymentEventPublisher eventPublisher) {
		this.repository = repository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public Payment processPaymentRequested(PaymentRequestedEvent event) {
		Payment payment = decide(event.orderId(), event.customerId(), event.amount());
		repository.save(payment);
		eventPublisher.publishProcessed(new PaymentProcessedEvent(
				UUID.randomUUID(), event.correlationId(), event.orderId(), Instant.now(),
				payment.getId(), payment.getStatus(), payment.getReason(), payment.getAmount()));
		return payment;
	}

	@Transactional
	public Payment processCompensationRequested(PaymentCompensationRequestedEvent event) {
		Payment payment = repository.findById(event.paymentId())
				.orElseThrow(() -> new PaymentNotFoundException(event.paymentId()));
		payment.setStatus(PaymentStatus.REFUNDED);
		payment.setUpdatedAt(Instant.now());
		repository.save(payment);
		eventPublisher.publishCompensated(new PaymentCompensatedEvent(
				UUID.randomUUID(), event.correlationId(), event.orderId(), Instant.now(),
				payment.getId(), payment.getStatus()));
		return payment;
	}

	@Transactional
	public Payment createPayment(UUID orderId, UUID customerId, BigDecimal amount) {
		return repository.save(decide(orderId, customerId, amount));
	}

	private Payment decide(UUID orderId, UUID customerId, BigDecimal amount) {
		Instant now = Instant.now();
		Payment payment = new Payment();
		payment.setId(UUID.randomUUID());
		payment.setOrderId(orderId);
		payment.setCustomerId(customerId);
		payment.setAmount(amount);
		payment.setCreatedAt(now);
		payment.setUpdatedAt(now);
		if (amount.compareTo(AUTHORIZATION_LIMIT) > 0) {
			payment.setStatus(PaymentStatus.REJECTED);
			payment.setReason(REJECTION_REASON);
		} else {
			payment.setStatus(PaymentStatus.APPROVED);
		}
		return payment;
	}

	@Transactional(readOnly = true)
	public Payment getPayment(UUID id) {
		return repository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public Page<Payment> listPayments(PaymentStatus status, UUID orderId, Pageable pageable) {
		if (status != null && orderId != null) {
			return repository.findByStatusAndOrderId(status, orderId, pageable);
		}
		if (status != null) {
			return repository.findByStatus(status, pageable);
		}
		if (orderId != null) {
			return repository.findByOrderId(orderId, pageable);
		}
		return repository.findAll(pageable);
	}

	@Transactional
	public Payment updatePayment(UUID id, PaymentStatus newStatus, String reason) {
		Payment payment = getPayment(id);
		if (newStatus != null && newStatus != payment.getStatus()) {
			Set<PaymentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(payment.getStatus(), Set.of());
			if (!allowed.contains(newStatus)) {
				throw new InvalidPaymentStateException(
						"Illegal transition from %s to %s".formatted(payment.getStatus(), newStatus));
			}
			payment.setStatus(newStatus);
		}
		if (reason != null) {
			payment.setReason(reason);
		}
		payment.setUpdatedAt(Instant.now());
		return repository.save(payment);
	}

	@Transactional
	public void deletePayment(UUID id) {
		Payment payment = getPayment(id);
		if (payment.getStatus() != PaymentStatus.PENDING) {
			throw new InvalidPaymentStateException("Only PENDING payments can be deleted");
		}
		repository.delete(payment);
	}
}
