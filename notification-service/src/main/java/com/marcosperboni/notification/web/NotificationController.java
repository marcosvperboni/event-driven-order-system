package com.marcosperboni.notification.web;

import com.marcosperboni.notification.application.NotificationService;
import com.marcosperboni.notification.domain.Notification;
import com.marcosperboni.notification.domain.NotificationStatus;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService service;

	public NotificationController(NotificationService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationRequest request) {
		Notification notification = service.send(request.orderId(), request.customerId(), request.channel(),
				request.message());
		return ResponseEntity.status(HttpStatus.CREATED).body(NotificationMapper.toResponse(notification));
	}

	@GetMapping
	public Page<NotificationResponse> list(
			@RequestParam(required = false) UUID orderId,
			@RequestParam(required = false) NotificationStatus status,
			Pageable pageable) {
		return service.list(orderId, status, pageable).map(NotificationMapper::toResponse);
	}

	@GetMapping("/{id}")
	public NotificationResponse get(@PathVariable UUID id) {
		return NotificationMapper.toResponse(service.getById(id));
	}

	@PutMapping("/{id}")
	public NotificationResponse update(@PathVariable UUID id, @RequestBody NotificationUpdateRequest request) {
		Notification notification = service.update(id, request.status(), request.message());
		return NotificationMapper.toResponse(notification);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
