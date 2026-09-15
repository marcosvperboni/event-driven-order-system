package com.marcosperboni.inventory.infrastructure.web;

import com.marcosperboni.inventory.domain.DuplicateSkuException;
import com.marcosperboni.inventory.domain.ProductNotFoundException;
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
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<FieldErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new FieldErrorItem(fe.getField(), fe.getDefaultMessage()))
				.toList();
		return build(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
	}

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(DuplicateSkuException.class)
	public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateSkuException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request, List.of());
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request, List<FieldErrorItem> errors) {
		ErrorResponse body = new ErrorResponse(
				Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), errors);
		return ResponseEntity.status(status).body(body);
	}

	public record ErrorResponse(
			Instant timestamp, int status, String error, String message, String path, List<FieldErrorItem> errors) {
	}

	public record FieldErrorItem(String field, String message) {
	}
}
