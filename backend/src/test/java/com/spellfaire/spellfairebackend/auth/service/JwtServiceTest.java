package com.spellfaire.spellfairebackend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.spellfaire.spellfairebackend.auth.model.User;

class JwtServiceTest {

	@Test
	void createAndVerifyAccessTokenIncludesExpectedClaims() {
		JwtService jwtService = new JwtService("unit-test-secret", "spellfaire-test", 3600);

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("jwt.tester@example.com");
		user.setUsername("JwtTester");

		String token = jwtService.createAccessToken(user);
		DecodedJWT decoded = jwtService.verify(token);

		assertNotNull(token);
		assertEquals("spellfaire-test", decoded.getIssuer());
		assertEquals(user.getId().toString(), decoded.getSubject());
		assertEquals(user.getEmail(), decoded.getClaim("email").asString());
		assertEquals(user.getUsername(), decoded.getClaim("username").asString());
	}

	@Test
	void verifyRejectsTamperedToken() {
		JwtService jwtService = new JwtService("unit-test-secret", "spellfaire-test", 3600);

		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("jwt.tester@example.com");
		user.setUsername("JwtTester");

		String token = jwtService.createAccessToken(user);
		String[] parts = token.split("\\.");
		String tampered = parts[0] + ".e30." + parts[2];

		assertThrows(JWTVerificationException.class, () -> jwtService.verify(tampered));
	}

	@Test
	void verifyRejectsTokenWithDifferentIssuer() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("jwt.tester@example.com");
		user.setUsername("JwtTester");

		JwtService issuerA = new JwtService("unit-test-secret", "issuer-a", 3600);
		JwtService issuerB = new JwtService("unit-test-secret", "issuer-b", 3600);

		String token = issuerA.createAccessToken(user);

		assertThrows(JWTVerificationException.class, () -> issuerB.verify(token));
	}
}
