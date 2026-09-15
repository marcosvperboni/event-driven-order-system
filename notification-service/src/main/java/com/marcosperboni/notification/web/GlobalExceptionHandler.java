package com.marcosperboni.notification.web;

import com.marcosperboni.notification.application.InvalidNotificationStateException;
import com.marcosperboni.notification.application.NotificationNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<ErrorResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldError)
				.toList();
		return build(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
	}

	@ExceptionHandler(NotificationNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NotificationNotFoundException ex,
			HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(InvalidNotificationStateException.class)
	public ResponseEntity<ErrorResponse> handleInvalidState(InvalidNotificationStateException ex,
			HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request, List.of());
	}

	private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
		return new ErrorResponse.FieldError(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
			List<ErrorResponse.FieldError> errors) {
		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI(),
				errors);
		return ResponseEntity.status(status).body(body);
	}
}
