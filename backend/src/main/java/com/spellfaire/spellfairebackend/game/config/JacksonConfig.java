package com.spellfaire.spellfairebackend.game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson configuration for JSON serialization/deserialization.
 */
@Configuration
public class JacksonConfig {

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
