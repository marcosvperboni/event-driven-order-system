package com.marcosperboni.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marcosperboni.payment.domain.Payment;
import com.marcosperboni.payment.domain.PaymentStatus;
import com.marcosperboni.payment.infrastructure.messaging.KafkaTopics;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentProcessedEvent;
import com.marcosperboni.payment.infrastructure.messaging.event.PaymentRequestedEvent;
import com.marcosperboni.payment.infrastructure.persistence.PaymentRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class PaymentEventFlowIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("payments_db")
			.withUsername("payments_user")
			.withPassword("payments_pass");

	@Container
	static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"));

	@Container
	static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Autowired
	private PaymentRepository paymentRepository;

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private KafkaProducer<String, String> producer;
	private KafkaConsumer<String, String> consumer;

	@BeforeEach
	void setUp() {
		Properties producerProps = new Properties();
		producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producer = new KafkaProducer<>(producerProps);

		Properties consumerProps = new Properties();
		consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
		consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumer = new KafkaConsumer<>(consumerProps);
		consumer.subscribe(List.of(KafkaTopics.PAYMENT_PROCESSED));
	}

	@AfterEach
	void tearDown() {
		producer.close();
		consumer.close();
	}

	@Test
	void consumesPaymentRequestedAndPublishesPaymentProcessed() throws Exception {
		UUID orderId = UUID.randomUUID();
		PaymentRequestedEvent event = new PaymentRequestedEvent(
				UUID.randomUUID(), orderId, orderId, Instant.now(), UUID.randomUUID(), new BigDecimal("250.00"));

		producer.send(new ProducerRecord<>(KafkaTopics.PAYMENT_REQUESTED, orderId.toString(),
				objectMapper.writeValueAsString(event))).get();

		PaymentProcessedEvent processed = pollForProcessedEvent();

		assertThat(processed.orderId()).isEqualTo(orderId);
		assertThat(processed.status()).isEqualTo(PaymentStatus.APPROVED);

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			Payment payment = paymentRepository.findById(processed.paymentId()).orElseThrow();
			assertThat(payment.getOrderId()).isEqualTo(orderId);
			assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
		});
	}

	private PaymentProcessedEvent pollForProcessedEvent() throws Exception {
		long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
		while (System.currentTimeMillis() < deadline) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
			for (ConsumerRecord<String, String> record : records) {
				return objectMapper.readValue(record.value(), PaymentProcessedEvent.class);
			}
		}
		throw new AssertionError("Timed out waiting for payment.processed.events");
	}
}
