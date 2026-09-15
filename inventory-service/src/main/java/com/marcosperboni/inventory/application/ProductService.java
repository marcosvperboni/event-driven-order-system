package com.marcosperboni.inventory.application;

import com.marcosperboni.inventory.domain.DuplicateSkuException;
import com.marcosperboni.inventory.domain.Product;
import com.marcosperboni.inventory.domain.ProductNotFoundException;
import com.marcosperboni.inventory.infrastructure.persistence.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public Product create(String sku, String name, int availableQuantity, BigDecimal unitPrice) {
		if (productRepository.existsBySku(sku)) {
			throw new DuplicateSkuException(sku);
		}
		Product product = new Product(UUID.randomUUID(), sku, name, availableQuantity, unitPrice);
		return productRepository.save(product);
	}

	@Transactional(readOnly = true)
	public Page<Product> list(String skuFilter, Pageable pageable) {
		if (StringUtils.hasText(skuFilter)) {
			return productRepository.findBySkuContainingIgnoreCase(skuFilter, pageable);
		}
		return productRepository.findAll(pageable);
	}

	@Transactional(readOnly = true)
	public Product get(UUID id) {
		return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	public Product update(UUID id, String name, int availableQuantity, BigDecimal unitPrice) {
		Product product = get(id);
		product.setName(name);
		product.setAvailableQuantity(availableQuantity);
		product.setUnitPrice(unitPrice);
		return product;
	}

	public void delete(UUID id) {
		if (!productRepository.existsById(id)) {
			throw new ProductNotFoundException(id);
		}
		productRepository.deleteById(id);
	}
}
