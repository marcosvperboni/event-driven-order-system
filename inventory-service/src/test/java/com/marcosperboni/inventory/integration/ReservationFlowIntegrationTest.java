package com.marcosperboni.inventory.integration;

import com.marcosperboni.inventory.domain.Product;
import com.marcosperboni.inventory.infrastructure.messaging.KafkaTopics;
import com.marcosperboni.inventory.infrastructure.messaging.event.InventoryReservationProcessedEvent;
import com.marcosperboni.inventory.infrastructure.messaging.event.InventoryReservationRequestedEvent;
import com.marcosperboni.inventory.infrastructure.messaging.event.ReservationItem;
import com.marcosperboni.inventory.infrastructure.messaging.event.ReservationStatus;
import com.marcosperboni.inventory.infrastructure.persistence.ProductRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ReservationFlowIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Container
	static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.7.1");

	@Container
	static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Autowired
	private ProductRepository productRepository;

	private KafkaTemplate<String, Object> testProducer;
	private org.apache.kafka.clients.consumer.Consumer<String, InventoryReservationProcessedEvent> testConsumer;

	@BeforeEach
	void setUp() {
		Map<String, Object> producerProps = Map.of(
				ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
				ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
				ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
		testProducer = new KafkaTemplate<>(producerFactory);

		Map<String, Object> consumerProps = Map.of(
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
				ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID(),
				ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
				JsonDeserializer.TRUSTED_PACKAGES, "com.marcosperboni.inventory.infrastructure.messaging.event",
				JsonDeserializer.VALUE_DEFAULT_TYPE, InventoryReservationProcessedEvent.class.getName(),
				JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
		testConsumer = new DefaultKafkaConsumerFactory<String, InventoryReservationProcessedEvent>(consumerProps)
				.createConsumer();
		testConsumer.subscribe(java.util.List.of(KafkaTopics.RESERVATION_PROCESSED));
		testConsumer.poll(Duration.ofMillis(0));
	}

	@AfterEach
	void tearDown() {
		testConsumer.close();
	}

	@Test
	void reservesStockAndPublishesReserved() {
		Product product = new Product(UUID.randomUUID(), "SKU-INT-1", "Test Product", 20, BigDecimal.TEN);
		productRepository.save(product);

		UUID orderId = UUID.randomUUID();
		publishReservationRequested(orderId, product.getId(), 5);

		ConsumerRecord<String, InventoryReservationProcessedEvent> record = awaitRecordForOrder(orderId);

		assertThat(record.value().getStatus()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(record.value().getOrderId()).isEqualTo(orderId);

		Product reloaded = productRepository.findById(product.getId()).orElseThrow();
		assertThat(reloaded.getAvailableQuantity()).isEqualTo(15);
	}

	@Test
	void rejectsAndLeavesStockUnchangedWhenQuantityExceedsStock() {
		Product product = new Product(UUID.randomUUID(), "SKU-INT-2", "Test Product 2", 3, BigDecimal.TEN);
		productRepository.save(product);

		UUID orderId = UUID.randomUUID();
		publishReservationRequested(orderId, product.getId(), 99);

		ConsumerRecord<String, InventoryReservationProcessedEvent> record = awaitRecordForOrder(orderId);

		assertThat(record.value().getStatus()).isEqualTo(ReservationStatus.REJECTED);

		Product reloaded = productRepository.findById(product.getId()).orElseThrow();
		assertThat(reloaded.getAvailableQuantity()).isEqualTo(3);
	}

	private void publishReservationRequested(UUID orderId, UUID productId, int quantity) {
		InventoryReservationRequestedEvent event = new InventoryReservationRequestedEvent();
		event.setEventId(UUID.randomUUID());
		event.setCorrelationId(orderId);
		event.setOrderId(orderId);
		event.setOccurredAt(Instant.now());
		event.setItems(java.util.List.of(new ReservationItem(productId, quantity)));
		testProducer.send(KafkaTopics.RESERVATION_REQUESTED, orderId.toString(), event);
	}

	/**
	 * Each test uses a fresh consumer group reading from "earliest", so the topic may still
	 * hold reply messages left over from a previous test method — filter by orderId instead of
	 * blindly taking the first polled record.
	 */
	private ConsumerRecord<String, InventoryReservationProcessedEvent> awaitRecordForOrder(UUID orderId) {
		long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
		while (System.currentTimeMillis() < deadline) {
			org.apache.kafka.clients.consumer.ConsumerRecords<String, InventoryReservationProcessedEvent> records =
					testConsumer.poll(Duration.ofMillis(500));
			for (ConsumerRecord<String, InventoryReservationProcessedEvent> record : records) {
				if (orderId.equals(record.value().getOrderId())) {
					return record;
				}
			}
		}
		throw new AssertionError("no reservation-processed record found for order " + orderId + " within timeout");
	}
}
