package com.marcosperboni.inventory.application;

import com.marcosperboni.inventory.domain.Product;
import com.marcosperboni.inventory.infrastructure.messaging.event.ReservationItem;
import com.marcosperboni.inventory.infrastructure.persistence.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

	private final ProductRepository productRepository;

	public ReservationService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * All-or-nothing stock reservation: validates every item first, decrements only if
	 * every product exists with sufficient stock, in a single transaction.
	 */
	@Transactional
	public ReservationResult reserve(List<ReservationItem> items) {
		for (ReservationItem item : items) {
			Optional<Product> product = productRepository.findById(item.getProductId());
			if (product.isEmpty()) {
				return ReservationResult.rejected("product " + item.getProductId() + " not found");
			}
			if (!product.get().hasEnoughStock(item.getQuantity())) {
				return ReservationResult.rejected("insufficient stock for product " + item.getProductId());
			}
		}

		for (ReservationItem item : items) {
			Product product = productRepository.findById(item.getProductId()).orElseThrow();
			product.decrementStock(item.getQuantity());
		}

		return ReservationResult.reserved();
	}
}
