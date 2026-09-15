package com.marcosperboni.inventory.infrastructure.web;

import tools.jackson.databind.ObjectMapper;
import com.marcosperboni.inventory.application.ProductService;
import com.marcosperboni.inventory.domain.DuplicateSkuException;
import com.marcosperboni.inventory.domain.Product;
import com.marcosperboni.inventory.domain.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ProductService productService;

	private Product sampleProduct() {
		return new Product(UUID.randomUUID(), "SKU-1", "Mouse", 10, new BigDecimal("29.90"));
	}

	@Test
	void createReturns201ForValidPayload() throws Exception {
		when(productService.create(anyString(), anyString(), anyInt(), any())).thenReturn(sampleProduct());

		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku":"SKU-1","name":"Mouse","availableQuantity":10,"unitPrice":29.90}
								"""))
				.andExpect(status().isCreated());
	}

	@Test
	void createReturns400ForBlankSku() throws Exception {
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku":"","name":"Mouse","availableQuantity":10,"unitPrice":29.90}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createReturns400ForBlankName() throws Exception {
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku":"SKU-1","name":"","availableQuantity":10,"unitPrice":29.90}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createReturns400ForNegativeQuantity() throws Exception {
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku":"SKU-1","name":"Mouse","availableQuantity":-1,"unitPrice":29.90}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createReturns409ForDuplicateSku() throws Exception {
		when(productService.create(anyString(), anyString(), anyInt(), any())).thenThrow(new DuplicateSkuException("SKU-1"));

		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sku":"SKU-1","name":"Mouse","availableQuantity":10,"unitPrice":29.90}
								"""))
				.andExpect(status().isConflict());
	}

	@Test
	void listReturns200() throws Exception {
		when(productService.list(any(), any())).thenReturn(new PageImpl<>(List.of(sampleProduct())));

		mockMvc.perform(get("/api/products"))
				.andExpect(status().isOk());
	}

	@Test
	void getReturns200ForExistingProduct() throws Exception {
		Product product = sampleProduct();
		when(productService.get(product.getId())).thenReturn(product);

		mockMvc.perform(get("/api/products/{id}", product.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sku").value("SKU-1"));
	}

	@Test
	void getReturns404ForUnknownProduct() throws Exception {
		UUID id = UUID.randomUUID();
		when(productService.get(id)).thenThrow(new ProductNotFoundException(id));

		mockMvc.perform(get("/api/products/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateReturns200ForExistingProduct() throws Exception {
		Product product = sampleProduct();
		when(productService.update(any(), anyString(), anyInt(), any())).thenReturn(product);

		mockMvc.perform(put("/api/products/{id}", product.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Mouse Pro","availableQuantity":20,"unitPrice":39.90}
								"""))
				.andExpect(status().isOk());
	}

	@Test
	void updateReturns404ForUnknownProduct() throws Exception {
		UUID id = UUID.randomUUID();
		when(productService.update(any(), anyString(), anyInt(), any())).thenThrow(new ProductNotFoundException(id));

		mockMvc.perform(put("/api/products/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Mouse Pro","availableQuantity":20,"unitPrice":39.90}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteReturns204ForExistingProduct() throws Exception {
		mockMvc.perform(delete("/api/products/{id}", UUID.randomUUID()))
				.andExpect(status().isNoContent());
	}

	@Test
	void deleteReturns404ForUnknownProduct() throws Exception {
		UUID id = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new ProductNotFoundException(id)).when(productService).delete(id);

		mockMvc.perform(delete("/api/products/{id}", id))
				.andExpect(status().isNotFound());
	}
}
