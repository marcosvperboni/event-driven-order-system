package com.marcosperboni.notification.integration;

import com.marcosperboni.notification.AbstractIntegrationTest;
import com.marcosperboni.notification.domain.Notification;
import com.marcosperboni.notification.domain.NotificationStatus;
import com.marcosperboni.notification.infrastructure.messaging.OrderStatusEvent;
import com.marcosperboni.notification.infrastructure.persistence.NotificationRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderStatusEventListenerIntegrationTest extends AbstractIntegrationTest {

	private static final String TOPIC = "order.status.events";

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private NotificationRepository repository;

	@Test
	void confirmedEventPersistsSentNotification() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		OrderStatusEvent event = new OrderStatusEvent(UUID.randomUUID(), orderId, orderId, Instant.now(),
				customerId, OrderStatusEvent.OrderStatus.CONFIRMED, null);

		kafkaTemplate.send(TOPIC, orderId.toString(), event);

		List<Notification> found = awaitNotification(orderId);
		assertThat(found).hasSize(1);
		assertThat(found.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
		assertThat(found.get(0).getMessage()).contains("confirmed");
	}

	@Test
	void cancelledEventMessageIncludesReason() throws InterruptedException {
		UUID orderId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		String reason = "inventory unavailable";
		OrderStatusEvent event = new OrderStatusEvent(UUID.randomUUID(), orderId, orderId, Instant.now(),
				customerId, OrderStatusEvent.OrderStatus.CANCELLED, reason);

		kafkaTemplate.send(TOPIC, orderId.toString(), event);

		List<Notification> found = awaitNotification(orderId);
		assertThat(found).hasSize(1);
		assertThat(found.get(0).getMessage()).contains(reason);
	}

	private List<Notification> awaitNotification(UUID orderId) throws InterruptedException {
		Duration timeout = Duration.ofSeconds(15);
		Instant deadline = Instant.now().plus(timeout);
		List<Notification> found;
		do {
			found = repository.findByOrderId(orderId, PageRequest.of(0, 10)).getContent();
			if (!found.isEmpty()) {
				return found;
			}
			Thread.sleep(200);
		} while (Instant.now().isBefore(deadline));
		return found;
	}
}
