package com.marcosperboni.inventory.domain;

public class DuplicateSkuException extends RuntimeException {

	public DuplicateSkuException(String sku) {
		super("sku already exists: " + sku);
	}
}
