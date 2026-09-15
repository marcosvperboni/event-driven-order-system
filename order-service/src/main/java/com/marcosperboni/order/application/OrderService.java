package com.marcosperboni.order.application;

import com.marcosperboni.order.domain.Order;
import com.marcosperboni.order.domain.OrderItem;
import com.marcosperboni.order.domain.OrderStatus;
import com.marcosperboni.order.domain.SagaLogEntry;
import com.marcosperboni.order.domain.exception.InvalidOrderStateException;
import com.marcosperboni.order.domain.exception.OrderNotFoundException;
import com.marcosperboni.order.infrastructure.messaging.KafkaTopics;
import com.marcosperboni.order.infrastructure.messaging.OrderEventPublisher;
import com.marcosperboni.order.infrastructure.messaging.event.OrderCreatedEvent;
import com.marcosperboni.order.infrastructure.persistence.OrderRepository;
import com.marcosperboni.order.infrastructure.persistence.SagaLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final SagaLogRepository sagaLogRepository;
	private final OrderEventPublisher eventPublisher;

	public OrderService(OrderRepository orderRepository, SagaLogRepository sagaLogRepository,
			OrderEventPublisher eventPublisher) {
		this.orderRepository = orderRepository;
		this.sagaLogRepository = sagaLogRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public Order createOrder(UUID customerId, List<OrderItem> items) {
		Instant now = Instant.now();
		Order order = new Order(UUID.randomUUID(), customerId, OrderStatus.PENDING, computeTotal(items), now, now);
		order.replaceItems(items);
		orderRepository.save(order);
		eventPublisher.publish(KafkaTopics.ORDER_CREATED, order.getId(), toOrderCreatedEvent(order));
		return order;
	}

	@Transactional(readOnly = true)
	public Page<Order> listOrders(OrderStatus status, Pageable pageable) {
		return status != null ? orderRepository.findByStatus(status, pageable) : orderRepository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public Order getOrder(UUID id) {
		return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
	}

	@Transactional
	public Order updateOrder(UUID id, UUID customerId, List<OrderItem> items) {
		Order order = getOrder(id);
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new InvalidOrderStateException("cannot update an order once its saga has started");
		}
		order.setCustomerId(customerId);
		order.replaceItems(items);
		order.setTotalAmount(computeTotal(items));
		order.setUpdatedAt(Instant.now());
		return orderRepository.save(order);
	}

	@Transactional
	public void deleteOrder(UUID id) {
		Order order = getOrder(id);
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new InvalidOrderStateException("cannot delete an order once its saga has started");
		}
		orderRepository.delete(order);
	}

	@Transactional(readOnly = true)
	public List<SagaLogEntry> getSagaLog(UUID orderId) {
		getOrder(orderId);
		return sagaLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
	}

	private BigDecimal computeTotal(List<OrderItem> items) {
		return items.stream()
				.map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private OrderCreatedEvent toOrderCreatedEvent(Order order) {
		List<OrderCreatedEvent.Item> items = order.getItems().stream()
				.map(item -> new OrderCreatedEvent.Item(item.getProductId(), item.getProductName(), item.getQuantity(),
						item.getUnitPrice()))
				.toList();
		return new OrderCreatedEvent(UUID.randomUUID(), order.getId(), order.getId(), Instant.now(),
				order.getCustomerId(), items, order.getTotalAmount());
	}
}
