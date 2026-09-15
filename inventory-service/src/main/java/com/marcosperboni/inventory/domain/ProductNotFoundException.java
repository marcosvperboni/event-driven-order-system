package com.marcosperboni.inventory.domain;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

	public ProductNotFoundException(UUID id) {
		super("product not found: " + id);
	}
}
