package com.spellfaire.spellfairebackend.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

class RestExceptionHandlerTest {

	private final RestExceptionHandler handler = new RestExceptionHandler();

	@Test
	void handleBadRequestUsesExceptionMessageAndPath() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/games");

		ResponseEntity<ApiError> response = handler.handleBadRequest(
				new IllegalArgumentException("Not enough mana"), request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Not enough mana", response.getBody().message());
		assertEquals("/api/games", response.getBody().path());
	}

	@Test
	void handleResponseStatusFallsBackToReasonPhraseWhenReasonIsNull() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/profile");
		ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND);

		ResponseEntity<ApiError> response = handler.handleResponseStatus(exception, request);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Not Found", response.getBody().message());
	}

	@Test
	void handleValidationAggregatesFieldErrors() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
		bindingResult.addError(new FieldError("registerRequest", "email", "must be a well-formed email address"));
		bindingResult.addError(new FieldError("registerRequest", "password", "must not be blank"));
		MethodParameter methodParameter = new MethodParameter(
				RestExceptionHandlerTest.class.getDeclaredMethod("dummyHandler", List.class),
				0);

		MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
				methodParameter,
				bindingResult);

		ResponseEntity<ApiError> response = handler.handleValidation(exception, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(
				"email: must be a well-formed email address; password: must not be blank",
				response.getBody().message());
	}

	@Test
	void handleOtherReturnsInternalServerError() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/boom");

		ResponseEntity<ApiError> response = handler.handleOther(new RuntimeException("boom"), request);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Internal server error", response.getBody().message());
	}

	@SuppressWarnings("unused")
	private void dummyHandler(List<String> ignored) {
		// helper method for constructing MethodArgumentNotValidException in tests
	}
}
