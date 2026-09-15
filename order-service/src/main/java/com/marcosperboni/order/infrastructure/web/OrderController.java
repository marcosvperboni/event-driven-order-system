package com.marcosperboni.order.infrastructure.web;

import com.marcosperboni.order.application.OrderService;
import com.marcosperboni.order.domain.Order;
import com.marcosperboni.order.domain.OrderStatus;
import com.marcosperboni.order.infrastructure.web.dto.OrderRequest;
import com.marcosperboni.order.infrastructure.web.dto.OrderResponse;
import com.marcosperboni.order.infrastructure.web.dto.SagaLogEntryResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
		Order order = orderService.createOrder(request.customerId(), OrderMapper.toItems(request.items()));
		return ResponseEntity.created(URI.create("/api/orders/" + order.getId())).body(OrderMapper.toResponse(order));
	}

	@GetMapping
	public Page<OrderResponse> list(@RequestParam(required = false) OrderStatus status, Pageable pageable) {
		return orderService.listOrders(status, pageable).map(OrderMapper::toResponse);
	}

	@GetMapping("/{id}")
	public OrderResponse get(@PathVariable UUID id) {
		return OrderMapper.toResponse(orderService.getOrder(id));
	}

	@PutMapping("/{id}")
	public OrderResponse update(@PathVariable UUID id, @Valid @RequestBody OrderRequest request) {
		Order order = orderService.updateOrder(id, request.customerId(), OrderMapper.toItems(request.items()));
		return OrderMapper.toResponse(order);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		orderService.deleteOrder(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/saga-log")
	public List<SagaLogEntryResponse> sagaLog(@PathVariable UUID id) {
		return orderService.getSagaLog(id).stream().map(OrderMapper::toResponse).toList();
	}
}
