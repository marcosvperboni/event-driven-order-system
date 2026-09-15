package com.marcosperboni.payment.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Boot 4's own Jackson autoconfiguration only exposes a Jackson 3
 * (tools.jackson.databind.ObjectMapper) bean. Kafka event payloads in this
 * service are parsed manually with classic Jackson 2, so that ObjectMapper
 * needs its own explicit bean.
 */
@Configuration
public class JacksonConfig {

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}
