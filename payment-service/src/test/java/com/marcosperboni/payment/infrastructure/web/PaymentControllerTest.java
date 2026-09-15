package com.marcosperboni.payment.infrastructure.web;

import tools.jackson.databind.ObjectMapper;
import com.marcosperboni.payment.application.PaymentService;
import com.marcosperboni.payment.domain.InvalidPaymentStateException;
import com.marcosperboni.payment.domain.Payment;
import com.marcosperboni.payment.domain.PaymentNotFoundException;
import com.marcosperboni.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PaymentService paymentService;

	private Payment samplePayment(PaymentStatus status) {
		Payment payment = new Payment();
		payment.setId(UUID.randomUUID());
		payment.setOrderId(UUID.randomUUID());
		payment.setCustomerId(UUID.randomUUID());
		payment.setAmount(new BigDecimal("100.00"));
		payment.setStatus(status);
		payment.setCreatedAt(Instant.now());
		payment.setUpdatedAt(Instant.now());
		return payment;
	}

	@Test
	void createReturns201OnValidRequest() throws Exception {
		Payment payment = samplePayment(PaymentStatus.APPROVED);
		when(paymentService.createPayment(any(), any(), any())).thenReturn(payment);

		PaymentRequest request = new PaymentRequest(payment.getOrderId(), payment.getCustomerId(), payment.getAmount());

		mockMvc.perform(post("/api/payments")
						.contentType("application/json")
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void createReturns400WhenAmountIsNotPositive() throws Exception {
		String body = """
				{"orderId":"%s","customerId":"%s","amount":0}
				""".formatted(UUID.randomUUID(), UUID.randomUUID());

		mockMvc.perform(post("/api/payments").contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createReturns400WhenOrderIdAndCustomerIdMissing() throws Exception {
		String body = """
				{"amount":100.00}
				""";

		mockMvc.perform(post("/api/payments").contentType("application/json").content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors", org.hamcrest.Matchers.hasSize(2)));
	}

	@Test
	void getReturns200WhenFound() throws Exception {
		Payment payment = samplePayment(PaymentStatus.APPROVED);
		when(paymentService.getPayment(payment.getId())).thenReturn(payment);

		mockMvc.perform(get("/api/payments/{id}", payment.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(payment.getId().toString()));
	}

	@Test
	void getReturns404WhenNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(paymentService.getPayment(id)).thenThrow(new PaymentNotFoundException(id));

		mockMvc.perform(get("/api/payments/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void listReturns200() throws Exception {
		Payment payment = samplePayment(PaymentStatus.APPROVED);
		Page<Payment> page = new PageImpl<>(List.of(payment), PageRequest.of(0, 20), 1);
		when(paymentService.listPayments(eq(null), eq(null), any())).thenReturn(page);

		mockMvc.perform(get("/api/payments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)));
	}

	@Test
	void putReturns200OnLegalTransition() throws Exception {
		Payment payment = samplePayment(PaymentStatus.REFUNDED);
		when(paymentService.updatePayment(eq(payment.getId()), eq(PaymentStatus.REFUNDED), any()))
				.thenReturn(payment);

		mockMvc.perform(put("/api/payments/{id}", payment.getId())
						.contentType("application/json")
						.content("{\"status\":\"REFUNDED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REFUNDED"));
	}

	@Test
	void putReturns404WhenNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(paymentService.updatePayment(eq(id), any(), any())).thenThrow(new PaymentNotFoundException(id));

		mockMvc.perform(put("/api/payments/{id}", id)
						.contentType("application/json")
						.content("{\"status\":\"REFUNDED\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void putReturns409OnIllegalTransition() throws Exception {
		UUID id = UUID.randomUUID();
		when(paymentService.updatePayment(eq(id), any(), any()))
				.thenThrow(new InvalidPaymentStateException("Illegal transition from REJECTED to APPROVED"));

		mockMvc.perform(put("/api/payments/{id}", id)
						.contentType("application/json")
						.content("{\"status\":\"APPROVED\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void deleteReturns204WhenPending() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(delete("/api/payments/{id}", id))
				.andExpect(status().isNoContent());
	}

	@Test
	void deleteReturns404WhenNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new PaymentNotFoundException(id)).when(paymentService).deletePayment(id);

		mockMvc.perform(delete("/api/payments/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteReturns409WhenNotPending() throws Exception {
		UUID id = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new InvalidPaymentStateException("Only PENDING payments can be deleted"))
				.when(paymentService).deletePayment(id);

		mockMvc.perform(delete("/api/payments/{id}", id))
				.andExpect(status().isConflict());
	}
}
