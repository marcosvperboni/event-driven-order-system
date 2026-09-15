package com.marcosperboni.inventory.application;

import com.marcosperboni.inventory.domain.Product;
import com.marcosperboni.inventory.infrastructure.messaging.event.ReservationItem;
import com.marcosperboni.inventory.infrastructure.messaging.event.ReservationStatus;
import com.marcosperboni.inventory.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Test
	void reservesStockWhenAllItemsAvailable() {
		ReservationService service = new ReservationService(productRepository);
		Product mouse = new Product(UUID.randomUUID(), "SKU-1", "Mouse", 10, BigDecimal.TEN);
		Product keyboard = new Product(UUID.randomUUID(), "SKU-2", "Keyboard", 5, BigDecimal.TEN);
		when(productRepository.findById(mouse.getId())).thenReturn(Optional.of(mouse));
		when(productRepository.findById(keyboard.getId())).thenReturn(Optional.of(keyboard));

		ReservationResult result = service.reserve(List.of(
				new ReservationItem(mouse.getId(), 3),
				new ReservationItem(keyboard.getId(), 2)));

		assertThat(result.status()).isEqualTo(ReservationStatus.RESERVED);
		assertThat(mouse.getAvailableQuantity()).isEqualTo(7);
		assertThat(keyboard.getAvailableQuantity()).isEqualTo(3);
	}

	@Test
	void rejectsAndLeavesStockUnchangedWhenOneItemInsufficient() {
		ReservationService service = new ReservationService(productRepository);
		Product mouse = new Product(UUID.randomUUID(), "SKU-1", "Mouse", 10, BigDecimal.TEN);
		Product keyboard = new Product(UUID.randomUUID(), "SKU-2", "Keyboard", 1, BigDecimal.TEN);
		when(productRepository.findById(mouse.getId())).thenReturn(Optional.of(mouse));
		when(productRepository.findById(keyboard.getId())).thenReturn(Optional.of(keyboard));

		ReservationResult result = service.reserve(List.of(
				new ReservationItem(mouse.getId(), 3),
				new ReservationItem(keyboard.getId(), 5)));

		assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
		assertThat(result.reason()).contains("insufficient stock");
		assertThat(mouse.getAvailableQuantity()).isEqualTo(10);
		assertThat(keyboard.getAvailableQuantity()).isEqualTo(1);
	}

	@Test
	void rejectsWhenProductUnknown() {
		ReservationService service = new ReservationService(productRepository);
		UUID unknownId = UUID.randomUUID();
		when(productRepository.findById(unknownId)).thenReturn(Optional.empty());

		ReservationResult result = service.reserve(List.of(new ReservationItem(unknownId, 1)));

		assertThat(result.status()).isEqualTo(ReservationStatus.REJECTED);
		assertThat(result.reason()).contains("not found");
	}
}
