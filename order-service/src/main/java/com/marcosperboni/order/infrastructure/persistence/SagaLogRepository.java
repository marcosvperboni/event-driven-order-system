package com.marcosperboni.order.infrastructure.persistence;

import com.marcosperboni.order.domain.SagaLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SagaLogRepository extends JpaRepository<SagaLogEntry, UUID> {

	List<SagaLogEntry> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
