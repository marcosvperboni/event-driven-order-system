package com.marcosperboni.order.infrastructure.persistence;

import com.marcosperboni.order.domain.Order;
import com.marcosperboni.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	// Overridden with an entity graph so `items` (LAZY by default) is available to
	// the mapper after the read-only transaction closes (spring.jpa.open-in-view is off).
	@EntityGraph(attributePaths = "items")
	@Override
	Optional<Order> findById(UUID id);

	@EntityGraph(attributePaths = "items")
	@Override
	Page<Order> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "items")
	Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
