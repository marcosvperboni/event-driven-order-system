package com.marcosperboni.payment.application;

import com.marcosperboni.payment.domain.Payment;
import com.marcosperboni.payment.domain.PaymentStatus;
import com.marcosperboni.payment.infrastructure.messaging.PaymentEventPublisher;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentCompensationRequestedEvent;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentProcessedEvent;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentRequestedEvent;
import com.marcosperboni.payment.infrastructure.persistence.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private PaymentRepository repository;

	@Mock
	private PaymentEventPublisher eventPublisher;

	private PaymentService paymentService;

	@BeforeEach
	void setUp() {
		paymentService = new PaymentService(repository, eventPublisher);
		when(repository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void approvesPaymentUnderAuthorizationLimit() {
		PaymentRequestedEvent event = new PaymentRequestedEvent(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
				UUID.randomUUID(), new BigDecimal("4999.99"));

		Payment payment = paymentService.processPaymentRequested(event);

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
		assertThat(payment.getReason()).isNull();

		ArgumentCaptor<PaymentProcessedEvent> captor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
		verify(eventPublisher).publishProcessed(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.APPROVED);
		assertThat(captor.getValue().paymentId()).isEqualTo(payment.getId());
	}

	@Test
	void rejectsPaymentOverAuthorizationLimit() {
		PaymentRequestedEvent event = new PaymentRequestedEvent(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
				UUID.randomUUID(), new BigDecimal("5000.01"));

		Payment payment = paymentService.processPaymentRequested(event);

		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
		assertThat(payment.getReason()).isEqualTo("amount exceeds authorization limit");

		ArgumentCaptor<PaymentProcessedEvent> captor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);
		verify(eventPublisher).publishProcessed(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.REJECTED);
		assertThat(captor.getValue().reason()).isEqualTo("amount exceeds authorization limit");
	}

	@Test
	void refundsPaymentOnCompensationRequest() {
		UUID paymentId = UUID.randomUUID();
		Payment existing = new Payment();
		existing.setId(paymentId);
		existing.setOrderId(UUID.randomUUID());
		existing.setCustomerId(UUID.randomUUID());
		existing.setAmount(new BigDecimal("100.00"));
		existing.setStatus(PaymentStatus.APPROVED);
		existing.setCreatedAt(Instant.now());
		existing.setUpdatedAt(Instant.now());
		when(repository.findById(paymentId)).thenReturn(Optional.of(existing));

		PaymentCompensationRequestedEvent event = new PaymentCompensationRequestedEvent(
				UUID.randomUUID(), UUID.randomUUID(), existing.getOrderId(), Instant.now(), paymentId);

		Payment refunded = paymentService.processCompensationRequested(event);

		assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
		verify(eventPublisher).publishCompensated(any());
	}
}
