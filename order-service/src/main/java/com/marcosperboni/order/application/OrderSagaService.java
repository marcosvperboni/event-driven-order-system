package com.marcosperboni.order.application;

import com.marcosperboni.order.domain.Order;
import com.marcosperboni.order.domain.OrderStatus;
import com.marcosperboni.order.domain.exception.OrderNotFoundException;
import com.marcosperboni.order.infrastructure.messaging.KafkaTopics;
import com.marcosperboni.order.infrastructure.messaging.OrderEventPublisher;
import com.marcosperboni.order.infrastructure.messaging.event.InventoryReservationProcessedEvent;
import com.marcosperboni.order.infrastructure.messaging.event.InventoryReservationRequestedEvent;
import com.marcosperboni.order.infrastructure.messaging.event.OrderCreatedEvent;
import com.marcosperboni.order.infrastructure.messaging.event.OrderStatusEvent;
import com.marcosperboni.order.infrastructure.messaging.event.PaymentCompensatedEvent;
import com.marcosperboni.order.infrastructure.messaging.event.PaymentCompensationRequestedEvent;
import com.marcosperboni.order.infrastructure.messaging.event.PaymentProcessedEvent;
import com.marcosperboni.order.infrastructure.messaging.event.PaymentRequestedEvent;
import com.marcosperboni.order.infrastructure.persistence.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Drives the order saga forward/backward in reaction to reply events from payment-service
 * and inventory-service. This is the orchestration heart of order-service.
 */
@Service
public class OrderSagaService {

	private final OrderRepository orderRepository;
	private final OrderEventPublisher eventPublisher;

	public OrderSagaService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
		this.orderRepository = orderRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public void handleOrderCreated(OrderCreatedEvent event) {
		Order order = findOrder(event.orderId());
		order.setStatus(OrderStatus.PAYMENT_PENDING);
		order.setUpdatedAt(Instant.now());
		orderRepository.save(order);

		eventPublisher.publish(KafkaTopics.PAYMENT_REQUESTED, order.getId(),
				new PaymentRequestedEvent(UUID.randomUUID(), order.getId(), order.getId(), Instant.now(),
						order.getCustomerId(), order.getTotalAmount()));
	}

	@Transactional
	public void handlePaymentProcessed(PaymentProcessedEvent event) {
		Order order = findOrder(event.orderId());

		if ("APPROVED".equals(event.status())) {
			order.setPaymentId(event.paymentId());
			order.setStatus(OrderStatus.INVENTORY_PENDING);
			order.setUpdatedAt(Instant.now());
			orderRepository.save(order);

			List<InventoryReservationRequestedEvent.Item> items = order.getItems().stream()
					.map(item -> new InventoryReservationRequestedEvent.Item(item.getProductId(), item.getQuantity()))
					.toList();
			eventPublisher.publish(KafkaTopics.INVENTORY_RESERVATION_REQUESTED, order.getId(),
					new InventoryReservationRequestedEvent(UUID.randomUUID(), order.getId(), order.getId(),
							Instant.now(), items));
		} else {
			order.setStatus(OrderStatus.CANCELLED);
			order.setUpdatedAt(Instant.now());
			orderRepository.save(order);

			eventPublisher.publish(KafkaTopics.ORDER_STATUS, order.getId(),
					new OrderStatusEvent(UUID.randomUUID(), order.getId(), order.getId(), Instant.now(),
							order.getCustomerId(), "CANCELLED", event.reason()));
		}
	}

	@Transactional
	public void handleInventoryReservationProcessed(InventoryReservationProcessedEvent event) {
		Order order = findOrder(event.orderId());

		if ("RESERVED".equals(event.status())) {
			order.setStatus(OrderStatus.CONFIRMED);
			order.setUpdatedAt(Instant.now());
			orderRepository.save(order);

			eventPublisher.publish(KafkaTopics.ORDER_STATUS, order.getId(),
					new OrderStatusEvent(UUID.randomUUID(), order.getId(), order.getId(), Instant.now(),
							order.getCustomerId(), "CONFIRMED", null));
		} else {
			order.setStatus(OrderStatus.COMPENSATING_PAYMENT);
			order.setUpdatedAt(Instant.now());
			orderRepository.save(order);

			eventPublisher.publish(KafkaTopics.PAYMENT_COMPENSATION_REQUESTED, order.getId(),
					new PaymentCompensationRequestedEvent(UUID.randomUUID(), order.getId(), order.getId(),
							Instant.now(), order.getPaymentId()));
		}
	}

	@Transactional
	public void handlePaymentCompensated(PaymentCompensatedEvent event) {
		Order order = findOrder(event.orderId());
		order.setStatus(OrderStatus.CANCELLED);
		order.setUpdatedAt(Instant.now());
		orderRepository.save(order);

		eventPublisher.publish(KafkaTopics.ORDER_STATUS, order.getId(),
				new OrderStatusEvent(UUID.randomUUID(), order.getId(), order.getId(), Instant.now(),
						order.getCustomerId(), "CANCELLED", "inventory reservation failed, payment refunded"));
	}

	private Order findOrder(UUID orderId) {
		return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
	}
}
