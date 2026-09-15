package com.marcosperboni.order.infrastructure.web;

import com.marcosperboni.order.domain.Order;
import com.marcosperboni.order.domain.OrderItem;
import com.marcosperboni.order.domain.SagaLogEntry;
import com.marcosperboni.order.infrastructure.web.dto.OrderItemRequest;
import com.marcosperboni.order.infrastructure.web.dto.OrderItemResponse;
import com.marcosperboni.order.infrastructure.web.dto.OrderResponse;
import com.marcosperboni.order.infrastructure.web.dto.SagaLogEntryResponse;

import java.util.List;
import java.util.UUID;

public final class OrderMapper {

	private OrderMapper() {
	}

	public static List<OrderItem> toItems(List<OrderItemRequest> requests) {
		return requests.stream()
				.map(r -> new OrderItem(UUID.randomUUID(), r.productId(), r.productName(), r.quantity(), r.unitPrice()))
				.toList();
	}

	public static OrderResponse toResponse(Order order) {
		List<OrderItemResponse> items = order.getItems().stream()
				.map(i -> new OrderItemResponse(i.getId(), i.getProductId(), i.getProductName(), i.getQuantity(),
						i.getUnitPrice()))
				.toList();
		return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus().name(),
				order.getTotalAmount(), order.getPaymentId(), items, order.getCreatedAt(), order.getUpdatedAt());
	}

	public static SagaLogEntryResponse toResponse(SagaLogEntry entry) {
		return new SagaLogEntryResponse(entry.getId(), entry.getOrderId(), entry.getEventType(), entry.getPayload(),
				entry.getCreatedAt());
	}
}
