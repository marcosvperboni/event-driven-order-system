package com.marcosperboni.order.infrastructure.messaging;

import com.marcosperboni.order.domain.SagaLogEntry;
import com.marcosperboni.order.infrastructure.persistence.SagaLogRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Component
public class SagaLogWriter {

	private final SagaLogRepository sagaLogRepository;
	private final ObjectMapper objectMapper;

	public SagaLogWriter(SagaLogRepository sagaLogRepository, ObjectMapper objectMapper) {
		this.sagaLogRepository = sagaLogRepository;
		this.objectMapper = objectMapper;
	}

	public void log(UUID orderId, String eventType, Object payload) {
		sagaLogRepository.save(SagaLogEntry.of(orderId, eventType, objectMapper.writeValueAsString(payload)));
	}
}
