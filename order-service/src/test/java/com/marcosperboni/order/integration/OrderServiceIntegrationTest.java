package com.marcosperboni.order.integration;

import com.marcosperboni.order.application.OrderService;
import com.marcosperboni.order.domain.OrderItem;
import com.marcosperboni.order.domain.SagaLogEntry;
import com.marcosperboni.order.infrastructure.messaging.KafkaTopics;
import com.marcosperboni.order.infrastructure.persistence.SagaLogRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Container
	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}

	@Autowired
	private OrderService orderService;

	@Autowired
	private SagaLogRepository sagaLogRepository;

	@Test
	void createOrder_publishesOrderCreatedEvent_andWritesSagaLog() {
		UUID customerId = UUID.randomUUID();
		List<OrderItem> items = List.of(
				new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "widget", 1, new BigDecimal("10.00")));

		var order = orderService.createOrder(customerId, items);

		Map<String, Object> consumerProps = new HashMap<>();
		consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		ConsumerRecord<String, String> record = null;
		try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
			consumer.subscribe(List.of(KafkaTopics.ORDER_CREATED));
			long deadline = System.currentTimeMillis() + 10_000;
			while (record == null && System.currentTimeMillis() < deadline) {
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
				for (ConsumerRecord<String, String> candidate : records) {
					if (candidate.key().equals(order.getId().toString())) {
						record = candidate;
						break;
					}
				}
			}
		}

		assertThat(record).as("order.created.events record for order %s", order.getId()).isNotNull();
		assertThat(record.value()).contains(customerId.toString());

		List<SagaLogEntry> logs = sagaLogRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());
		assertThat(logs).isNotEmpty();
		assertThat(logs.get(0).getEventType()).isEqualTo(KafkaTopics.ORDER_CREATED);
	}

	@Test
	void getOrder_itemsAreAccessibleOutsideTheCreatingTransaction() {
		UUID customerId = UUID.randomUUID();
		List<OrderItem> items = List.of(
				new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "widget", 2, new BigDecimal("5.00")));

		var created = orderService.createOrder(customerId, items);

		// createOrder()'s @Transactional has already committed and closed by the time control
		// returns here; a fresh, separate @Transactional(readOnly = true) read (as the REST
		// controller performs on GET /api/orders/{id}) must still be able to read `items`
		// without a LazyInitializationException, since open-in-view is disabled.
		var reloaded = orderService.getOrder(created.getId());

		assertThat(reloaded.getItems()).hasSize(1);
		assertThat(reloaded.getItems().get(0).getProductName()).isEqualTo("widget");
	}
}
