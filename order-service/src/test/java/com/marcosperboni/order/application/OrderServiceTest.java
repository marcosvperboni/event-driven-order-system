package com.marcosperboni.order.application;

import com.marcosperboni.order.domain.Order;
import com.marcosperboni.order.domain.OrderItem;
import com.marcosperboni.order.domain.OrderStatus;
import com.marcosperboni.order.domain.exception.InvalidOrderStateException;
import com.marcosperboni.order.domain.exception.OrderNotFoundException;
import com.marcosperboni.order.infrastructure.messaging.OrderEventPublisher;
import com.marcosperboni.order.infrastructure.persistence.OrderRepository;
import com.marcosperboni.order.infrastructure.persistence.SagaLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private SagaLogRepository sagaLogRepository;
	@Mock
	private OrderEventPublisher eventPublisher;

	private OrderService orderService;

	@BeforeEach
	void setUp() {
		orderService = new OrderService(orderRepository, sagaLogRepository, eventPublisher);
	}

	@Test
	void createOrder_persistsPendingOrderAndPublishesEvent() {
		UUID customerId = UUID.randomUUID();
		List<OrderItem> items = List.of(new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "widget", 2,
				new BigDecimal("10.00")));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

		Order created = orderService.createOrder(customerId, items);

		assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(created.getTotalAmount()).isEqualByComparingTo("20.00");
		assertThat(created.getCustomerId()).isEqualTo(customerId);

		ArgumentCaptor<Order> savedCaptor = ArgumentCaptor.forClass(Order.class);
		verify(orderRepository).save(savedCaptor.capture());
		assertThat(savedCaptor.getValue().getItems()).hasSize(1);

		verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("order.created.events"), any(UUID.class),
				any());
	}

	@Test
	void updateOrder_whenNotPending_throwsConflict() {
		UUID id = UUID.randomUUID();
		Order order = new Order(id, UUID.randomUUID(), OrderStatus.CONFIRMED, BigDecimal.TEN, Instant.now(),
				Instant.now());
		when(orderRepository.findById(id)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.updateOrder(id, UUID.randomUUID(), List.of()))
				.isInstanceOf(InvalidOrderStateException.class);
		verify(orderRepository, never()).save(any());
	}

	@Test
	void deleteOrder_whenNotPending_throwsConflict() {
		UUID id = UUID.randomUUID();
		Order order = new Order(id, UUID.randomUUID(), OrderStatus.PAYMENT_PENDING, BigDecimal.TEN, Instant.now(),
				Instant.now());
		when(orderRepository.findById(id)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> orderService.deleteOrder(id)).isInstanceOf(InvalidOrderStateException.class);
		verify(orderRepository, never()).delete(any());
	}

	@Test
	void getOrder_whenMissing_throwsNotFound() {
		UUID id = UUID.randomUUID();
		when(orderRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> orderService.getOrder(id)).isInstanceOf(OrderNotFoundException.class);
	}
}
