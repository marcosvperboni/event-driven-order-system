package com.marcosperboni.inventory.infrastructure.persistence;

import com.marcosperboni.inventory.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	boolean existsBySku(String sku);

	Optional<Product> findBySku(String sku);

	Page<Product> findBySkuContainingIgnoreCase(String sku, Pageable pageable);
}
