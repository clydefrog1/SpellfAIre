package com.spellfaire.spellfairebackend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIT {
	private static final String REFRESH_COOKIE_NAME = "spellfaire_refresh";
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Container
	@SuppressWarnings("resource")
	private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("spellfaire_test")
			.withUsername("test")
			.withPassword("test");

	@DynamicPropertySource
	static void configureDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
	}

	@LocalServerPort
	private int port;

	private String url(String path) {
		return "http://localhost:" + port + path;
	}

	@Test
	void registerThenAccessMeWithBearerToken() throws Exception {
		Map<String, Object> registerBody = Map.of(
				"email", "integration.player@example.com",
				"username", "IntegrationHero",
				"password", "password-123");

		HttpRequest registerRequest = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/auth/register")))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(registerBody)))
				.build();

		HttpResponse<String> registerResponse = HTTP_CLIENT.send(registerRequest, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, registerResponse.statusCode());
		List<String> setCookieHeaders = registerResponse.headers().allValues(HttpHeaders.SET_COOKIE);
		assertTrue(setCookieHeaders.stream().anyMatch(value -> value.contains(REFRESH_COOKIE_NAME + "=")));
		assertTrue(setCookieHeaders.stream().anyMatch(value -> value.contains("HttpOnly")));

		@SuppressWarnings("unchecked")
		Map<String, Object> authResponse = OBJECT_MAPPER.readValue(registerResponse.body(), Map.class);
		String accessToken = (String) authResponse.get("accessToken");
		assertNotNull(accessToken);

		HttpRequest meRequest = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/auth/me")))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.GET()
				.build();

		HttpResponse<String> meResponse = HTTP_CLIENT.send(meRequest, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, meResponse.statusCode());

		@SuppressWarnings("unchecked")
		Map<String, Object> mePayload = OBJECT_MAPPER.readValue(meResponse.body(), Map.class);
		assertEquals("integration.player@example.com", mePayload.get("email"));
		assertEquals("IntegrationHero", mePayload.get("username"));
	}

	@Test
	void registerRejectsDuplicateEmail() throws Exception {
		Map<String, Object> body = Map.of(
				"email", "duplicate.player@example.com",
				"username", "PlayerOne",
				"password", "password-123");
		String payload = OBJECT_MAPPER.writeValueAsString(body);

		HttpRequest firstRequest = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/auth/register")))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();

		HttpResponse<String> firstResponse = HTTP_CLIENT.send(firstRequest, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, firstResponse.statusCode());

		HttpRequest secondRequest = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/auth/register")))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();

		HttpResponse<String> secondResponse = HTTP_CLIENT.send(secondRequest, HttpResponse.BodyHandlers.ofString());
		assertEquals(409, secondResponse.statusCode());
	}

	@Test
	void meRequiresAuthentication() throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/auth/me")))
				.GET()
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(403, response.statusCode());
	}
}
