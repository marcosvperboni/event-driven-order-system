package com.marcosperboni.order.infrastructure.web;

import com.marcosperboni.order.application.OrderService;
import com.marcosperboni.order.domain.Order;
import com.marcosperboni.order.domain.OrderItem;
import com.marcosperboni.order.domain.OrderStatus;
import com.marcosperboni.order.domain.exception.InvalidOrderStateException;
import com.marcosperboni.order.domain.exception.OrderNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService orderService;

	private Order sampleOrder(OrderStatus status) {
		Order order = new Order(UUID.randomUUID(), UUID.randomUUID(), status, new BigDecimal("20.00"), Instant.now(),
				Instant.now());
		order.addItem(new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "widget", 2, new BigDecimal("10.00")));
		return order;
	}

	private String validOrderJson() {
		return """
				{
				  "customerId": "%s",
				  "items": [
				    {"productId": "%s", "productName": "widget", "quantity": 2, "unitPrice": 10.00}
				  ]
				}
				""".formatted(UUID.randomUUID(), UUID.randomUUID());
	}

	@Test
	void create_valid_returns201() throws Exception {
		Order created = sampleOrder(OrderStatus.PENDING);
		when(orderService.createOrder(any(), any())).thenReturn(created);

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(validOrderJson()))
				.andExpect(status().isCreated());
	}

	@Test
	void create_emptyItems_returns400() throws Exception {
		String body = """
				{"customerId": "%s", "items": []}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_missingCustomerId_returns400() throws Exception {
		String body = """
				{"items": [{"productId": "%s", "productName": "widget", "quantity": 1, "unitPrice": 10.00}]}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_quantityZero_returns400() throws Exception {
		String body = """
				{"customerId": "%s", "items": [{"productId": "%s", "productName": "widget", "quantity": 0, "unitPrice": 10.00}]}
				""".formatted(UUID.randomUUID(), UUID.randomUUID());

		mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void get_unknown_returns404() throws Exception {
		UUID id = UUID.randomUUID();
		when(orderService.getOrder(eq(id))).thenThrow(new OrderNotFoundException(id));

		mockMvc.perform(get("/api/orders/{id}", id)).andExpect(status().isNotFound());
	}

	@Test
	void get_existing_returns200() throws Exception {
		Order order = sampleOrder(OrderStatus.PENDING);
		when(orderService.getOrder(eq(order.getId()))).thenReturn(order);

		mockMvc.perform(get("/api/orders/{id}", order.getId())).andExpect(status().isOk());
	}

	@Test
	void list_returns200() throws Exception {
		Page<Order> page = new PageImpl<>(List.of(sampleOrder(OrderStatus.PENDING)));
		when(orderService.listOrders(eq(null), any())).thenReturn(page);

		mockMvc.perform(get("/api/orders")).andExpect(status().isOk());
	}

	@Test
	void put_unknown_returns404() throws Exception {
		UUID id = UUID.randomUUID();
		when(orderService.updateOrder(eq(id), any(), any())).thenThrow(new OrderNotFoundException(id));

		mockMvc.perform(put("/api/orders/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validOrderJson()))
				.andExpect(status().isNotFound());
	}

	@Test
	void put_nonPending_returns409() throws Exception {
		UUID id = UUID.randomUUID();
		when(orderService.updateOrder(eq(id), any(), any()))
				.thenThrow(new InvalidOrderStateException("cannot update an order once its saga has started"));

		mockMvc.perform(put("/api/orders/{id}", id).contentType(MediaType.APPLICATION_JSON).content(validOrderJson()))
				.andExpect(status().isConflict());
	}

	@Test
	void delete_unknown_returns404() throws Exception {
		UUID id = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new OrderNotFoundException(id)).when(orderService).deleteOrder(eq(id));

		mockMvc.perform(delete("/api/orders/{id}", id)).andExpect(status().isNotFound());
	}

	@Test
	void delete_nonPending_returns409() throws Exception {
		UUID id = UUID.randomUUID();
		org.mockito.Mockito
				.doThrow(new InvalidOrderStateException("cannot delete an order once its saga has started"))
				.when(orderService).deleteOrder(eq(id));

		mockMvc.perform(delete("/api/orders/{id}", id)).andExpect(status().isConflict());
	}

	@Test
	void delete_pending_returns204() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(delete("/api/orders/{id}", id)).andExpect(status().isNoContent());
	}
}
