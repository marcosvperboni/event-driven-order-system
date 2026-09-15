package com.marcosperboni.order.application;

import com.marcosperboni.order.domain.Order;
import com.marcosperboni.order.domain.OrderItem;
import com.marcosperboni.order.domain.OrderStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSagaServiceTest {

	@Mock
	private OrderRepository orderRepository;
	@Mock
	private OrderEventPublisher eventPublisher;

	private OrderSagaService sagaService;

	@BeforeEach
	void setUp() {
		sagaService = new OrderSagaService(orderRepository, eventPublisher);
	}

	private Order pendingOrder(UUID orderId, OrderStatus status) {
		Order order = new Order(orderId, UUID.randomUUID(), status, new BigDecimal("20.00"), Instant.now(),
				Instant.now());
		order.addItem(new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "widget", 2, new BigDecimal("10.00")));
		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
		return order;
	}

	@Test
	void handleOrderCreated_movesToPaymentPendingAndPublishesPaymentRequested() {
		UUID orderId = UUID.randomUUID();
		Order order = pendingOrder(orderId, OrderStatus.PENDING);

		sagaService.handleOrderCreated(
				new OrderCreatedEvent(UUID.randomUUID(), orderId, orderId, Instant.now(), order.getCustomerId(),
						List.of(), order.getTotalAmount()));

		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
		verify(eventPublisher).publish(eq("payment.requested.events"), eq(orderId), any(PaymentRequestedEvent.class));
	}

	@Test
	void handlePaymentProcessed_approved_movesToInventoryPendingAndPublishesReservationRequest() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		Order order = pendingOrder(orderId, OrderStatus.PAYMENT_PENDING);

		sagaService.handlePaymentProcessed(new PaymentProcessedEvent(UUID.randomUUID(), orderId, orderId,
				Instant.now(), paymentId, "APPROVED", null, order.getTotalAmount()));

		assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_PENDING);
		assertThat(order.getPaymentId()).isEqualTo(paymentId);
		verify(eventPublisher).publish(eq("inventory.reservation.requested.events"), eq(orderId),
				any(InventoryReservationRequestedEvent.class));
	}

	@Test
	void handlePaymentProcessed_rejected_movesToCancelledAndPublishesOrderStatus() {
		UUID orderId = UUID.randomUUID();
		Order order = pendingOrder(orderId, OrderStatus.PAYMENT_PENDING);

		sagaService.handlePaymentProcessed(new PaymentProcessedEvent(UUID.randomUUID(), orderId, orderId,
				Instant.now(), UUID.randomUUID(), "REJECTED", "insufficient funds", order.getTotalAmount()));

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		ArgumentCaptor<OrderStatusEvent> captor = ArgumentCaptor.forClass(OrderStatusEvent.class);
		verify(eventPublisher).publish(eq("order.status.events"), eq(orderId), captor.capture());
		assertThat(captor.getValue().status()).isEqualTo("CANCELLED");
		assertThat(captor.getValue().reason()).isEqualTo("insufficient funds");
	}

	@Test
	void handleInventoryReservationProcessed_reserved_movesToConfirmed() {
		UUID orderId = UUID.randomUUID();
		Order order = pendingOrder(orderId, OrderStatus.INVENTORY_PENDING);

		sagaService.handleInventoryReservationProcessed(
				new InventoryReservationProcessedEvent(UUID.randomUUID(), orderId, orderId, Instant.now(), "RESERVED",
						null));

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
		ArgumentCaptor<OrderStatusEvent> captor = ArgumentCaptor.forClass(OrderStatusEvent.class);
		verify(eventPublisher).publish(eq("order.status.events"), eq(orderId), captor.capture());
		assertThat(captor.getValue().status()).isEqualTo("CONFIRMED");
	}

	@Test
	void handleInventoryReservationProcessed_rejected_startsCompensation() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		Order order = pendingOrder(orderId, OrderStatus.INVENTORY_PENDING);
		order.setPaymentId(paymentId);

		sagaService.handleInventoryReservationProcessed(
				new InventoryReservationProcessedEvent(UUID.randomUUID(), orderId, orderId, Instant.now(), "REJECTED",
						"out of stock"));

		assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPENSATING_PAYMENT);
		ArgumentCaptor<PaymentCompensationRequestedEvent> captor =
				ArgumentCaptor.forClass(PaymentCompensationRequestedEvent.class);
		verify(eventPublisher).publish(eq("payment.compensation.requested.events"), eq(orderId), captor.capture());
		assertThat(captor.getValue().paymentId()).isEqualTo(paymentId);
	}

	@Test
	void handlePaymentCompensated_movesToCancelledWithFixedReason() {
		UUID orderId = UUID.randomUUID();
		Order order = pendingOrder(orderId, OrderStatus.COMPENSATING_PAYMENT);

		sagaService.handlePaymentCompensated(
				new PaymentCompensatedEvent(UUID.randomUUID(), orderId, orderId, Instant.now(), UUID.randomUUID(),
						"REFUNDED"));

		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		ArgumentCaptor<OrderStatusEvent> captor = ArgumentCaptor.forClass(OrderStatusEvent.class);
		verify(eventPublisher).publish(eq("order.status.events"), eq(orderId), captor.capture());
		assertThat(captor.getValue().reason()).isEqualTo("inventory reservation failed, payment refunded");
	}
}
