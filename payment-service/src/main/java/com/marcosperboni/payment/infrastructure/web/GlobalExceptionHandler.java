package com.marcosperboni.payment.infrastructure.web;

import com.marcosperboni.payment.domain.InvalidPaymentStateException;
import com.marcosperboni.payment.domain.PaymentNotFoundException;
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
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest().body(new ErrorResponse(
				Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", "Validation failed",
				request.getRequestURI(), fieldErrors));
	}

	@ExceptionHandler(PaymentNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
				Instant.now(), HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(),
				request.getRequestURI(), List.of()));
	}

	@ExceptionHandler(InvalidPaymentStateException.class)
	public ResponseEntity<ErrorResponse> handleConflict(InvalidPaymentStateException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
				Instant.now(), HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(),
				request.getRequestURI(), List.of()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
				Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex.getMessage(),
				request.getRequestURI(), List.of()));
	}
}
