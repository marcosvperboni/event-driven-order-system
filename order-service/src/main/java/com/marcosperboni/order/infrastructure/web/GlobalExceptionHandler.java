package com.marcosperboni.order.infrastructure.web;

import com.marcosperboni.order.domain.exception.InvalidOrderStateException;
import com.marcosperboni.order.domain.exception.OrderNotFoundException;
import com.marcosperboni.order.infrastructure.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
				.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest().body(new ErrorResponse(Instant.now(), 400, "Bad Request",
				"Validation failed", request.getRequestURI(), errors));
	}

	@ExceptionHandler(OrderNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(Instant.now(), 404, "Not Found",
				ex.getMessage(), request.getRequestURI(), List.of()));
	}

	@ExceptionHandler(InvalidOrderStateException.class)
	public ResponseEntity<ErrorResponse> handleConflict(InvalidOrderStateException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(Instant.now(), 409, "Conflict",
				ex.getMessage(), request.getRequestURI(), List.of()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(Instant.now(), 500,
				"Internal Server Error", ex.getMessage(), request.getRequestURI(), List.of()));
	}
}
