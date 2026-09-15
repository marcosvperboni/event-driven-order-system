package com.marcosperboni.notification.application;

import com.marcosperboni.notification.domain.Notification;
import com.marcosperboni.notification.domain.NotificationChannel;
import com.marcosperboni.notification.domain.NotificationStatus;
import com.marcosperboni.notification.infrastructure.persistence.NotificationRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository repository;

	private NotificationService service;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		service = new NotificationService(repository);
	}

	@Test
	void confirmedOrderProducesSentNotificationWithConfirmationMessage() {
		UUID orderId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		String message = "Your order " + orderId + " has been confirmed!";
		when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Notification result = service.send(orderId, customerId, NotificationChannel.EMAIL, message);

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
		assertThat(captor.getValue().getMessage()).isEqualTo(message);
		assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
	}

	@Test
	void cancelledOrderMessageIncludesReason() {
		UUID orderId = UUID.randomUUID();
		UUID customerId = UUID.randomUUID();
		String reason = "inventory unavailable";
		String message = "Your order " + orderId + " was cancelled: " + reason;
		when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Notification result = service.send(orderId, customerId, NotificationChannel.EMAIL, message);

		assertThat(result.getMessage()).contains(reason);
		assertThat(result.getStatus()).isEqualTo(NotificationStatus.SENT);
	}

	@Test
	void updateRejectsTransitionWhenNotPending() {
		UUID id = UUID.randomUUID();
		Notification existing = Notification.create(UUID.randomUUID(), UUID.randomUUID(), NotificationChannel.EMAIL,
				"hi", NotificationStatus.SENT);
		when(repository.findById(id)).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.update(id, NotificationStatus.FAILED, null))
				.isInstanceOf(InvalidNotificationStateException.class);
	}

	@Test
	void updateThrowsNotFoundWhenMissing() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(id)).isInstanceOf(NotificationNotFoundException.class);
	}
}
