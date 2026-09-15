package com.marcosperboni.payment.infrastructure.web;

import com.marcosperboni.payment.application.PaymentService;
import com.marcosperboni.payment.domain.Payment;
import com.marcosperboni.payment.domain.PaymentStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping
	public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
		Payment payment = paymentService.createPayment(request.orderId(), request.customerId(), request.amount());
		return ResponseEntity.status(HttpStatus.CREATED).body(PaymentMapper.toResponse(payment));
	}

	@GetMapping
	public Page<PaymentResponse> list(
			@RequestParam(required = false) PaymentStatus status,
			@RequestParam(required = false) UUID orderId,
			Pageable pageable) {
		return paymentService.listPayments(status, orderId, pageable).map(PaymentMapper::toResponse);
	}

	@GetMapping("/{id}")
	public PaymentResponse get(@PathVariable UUID id) {
		return PaymentMapper.toResponse(paymentService.getPayment(id));
	}

	@PutMapping("/{id}")
	public PaymentResponse update(@PathVariable UUID id, @RequestBody PaymentUpdateRequest request) {
		return PaymentMapper.toResponse(paymentService.updatePayment(id, request.status(), request.reason()));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		paymentService.deletePayment(id);
		return ResponseEntity.noContent().build();
	}
}
