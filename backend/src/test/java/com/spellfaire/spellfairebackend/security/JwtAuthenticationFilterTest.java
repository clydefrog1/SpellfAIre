package com.spellfaire.spellfairebackend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.spellfaire.spellfairebackend.auth.service.JwtService;

import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private FilterChain filterChain;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void skipsAuthPathsWithoutCallingJwtVerifier() throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

		filter.doFilter(request, new MockHttpServletResponse(), filterChain);

		verify(jwtService, never()).verify(anyString());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void keepsAnonymousWhenAuthorizationHeaderIsMissing() throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games");

		filter.doFilter(request, new MockHttpServletResponse(), filterChain);

		verify(jwtService, never()).verify(anyString());
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void setsSecurityContextForValidBearerToken() throws Exception {
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/games");
		request.addHeader("Authorization", "Bearer valid-token");

		DecodedJWT decoded = JWT.require(Algorithm.HMAC256("test-secret"))
				.build()
				.verify(JWT.create().withSubject("user-123").sign(Algorithm.HMAC256("test-secret")));
		when(jwtService.verify("valid-token")).thenReturn(decoded);

		filter.doFilter(request, new MockHttpServletResponse(), filterChain);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(authentication);
		assertEquals("user-123", authentication.getPrincipal());
	}
}
