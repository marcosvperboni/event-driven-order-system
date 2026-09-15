package com.marcosperboni.inventory.infrastructure.messaging;

import com.marcosperboni.inventory.application.ReservationResult;
import com.marcosperboni.inventory.application.ReservationService;
import com.marcosperboni.inventory.infrastructure.messaging.event.InventoryReservationProcessedEvent;
import com.marcosperboni.inventory.infrastructure.messaging.event.InventoryReservationRequestedEvent;
import com.marcosperboni.inventory.infrastructure.messaging.event.ReservationItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationRequestListenerTest {

	@Mock
	private ReservationService reservationService;

	@Mock
	private ReservationEventPublisher publisher;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private InventoryReservationRequestedEvent requestedEvent() {
		InventoryReservationRequestedEvent event = new InventoryReservationRequestedEvent();
		event.setEventId(UUID.randomUUID());
		event.setCorrelationId(UUID.randomUUID());
		event.setOrderId(UUID.randomUUID());
		event.setOccurredAt(Instant.now());
		event.setItems(List.of(new ReservationItem(UUID.randomUUID(), 2)));
		return event;
	}

	@Test
	void processesAndPublishesOnFirstDelivery() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
		when(reservationService.reserve(any())).thenReturn(ReservationResult.reserved());

		ReservationRequestListener listener = new ReservationRequestListener(reservationService, publisher, redisTemplate);
		listener.onReservationRequested(requestedEvent());

		verify(reservationService).reserve(any());
		verify(publisher).publishProcessed(any(InventoryReservationProcessedEvent.class));
	}

	@Test
	void skipsProcessingOnDuplicateDelivery() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

		ReservationRequestListener listener = new ReservationRequestListener(reservationService, publisher, redisTemplate);
		listener.onReservationRequested(requestedEvent());

		verifyNoInteractions(reservationService, publisher);
	}
}
