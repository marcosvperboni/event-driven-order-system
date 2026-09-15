package com.marcosperboni.inventory.infrastructure.web;

import com.marcosperboni.inventory.application.ProductService;
import com.marcosperboni.inventory.domain.Product;
import com.marcosperboni.inventory.infrastructure.web.dto.ProductRequest;
import com.marcosperboni.inventory.infrastructure.web.dto.ProductResponse;
import com.marcosperboni.inventory.infrastructure.web.dto.ProductUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductResponse create(@Valid @RequestBody ProductRequest request) {
		Product product = productService.create(request.sku(), request.name(), request.availableQuantity(), request.unitPrice());
		return ProductResponse.from(product);
	}

	@GetMapping
	public Page<ProductResponse> list(@RequestParam(required = false) String sku, Pageable pageable) {
		return productService.list(sku, pageable).map(ProductResponse::from);
	}

	@GetMapping("/{id}")
	public ProductResponse get(@PathVariable UUID id) {
		return ProductResponse.from(productService.get(id));
	}

	@PutMapping("/{id}")
	public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request) {
		Product product = productService.update(id, request.name(), request.availableQuantity(), request.unitPrice());
		return ProductResponse.from(product);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		productService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
