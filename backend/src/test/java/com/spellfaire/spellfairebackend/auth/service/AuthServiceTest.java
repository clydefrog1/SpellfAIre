package com.spellfaire.spellfairebackend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.spellfaire.spellfairebackend.auth.dto.LoginRequest;
import com.spellfaire.spellfairebackend.auth.dto.RegisterRequest;
import com.spellfaire.spellfairebackend.auth.model.RefreshToken;
import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@InjectMocks
	private AuthService authService;

	private User existingUser;

	@BeforeEach
	void setUp() {
		existingUser = new User();
		existingUser.setId(UUID.randomUUID());
		existingUser.setEmail("player@example.com");
		existingUser.setUsername("PlayerOne");
		existingUser.setPasswordHash("hashed");
		existingUser.setCreatedAt(Instant.now());
	}

	@Test
	void registerNormalizesEmailAndCreatesUser() {
		RegisterRequest request = new RegisterRequest();
		request.setEmail("  PLAYER@Example.com ");
		request.setUsername("  NewPlayer  ");
		request.setPassword("password-123");

		when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password-123")).thenReturn("encoded-password");

		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User persisted = invocation.getArgument(0);
			persisted.setId(UUID.randomUUID());
			return persisted;
		});
		when(jwtService.createAccessToken(any(User.class))).thenReturn("access-token");

		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setTokenHash("token-hash");
		refreshToken.setUser(existingUser);
		when(refreshTokenService.issueForUser(any(User.class)))
				.thenReturn(new RefreshTokenService.IssuedRefreshToken("raw-refresh-token", refreshToken));

		AuthService.AuthResult result = authService.register(request);

		assertEquals("access-token", result.response().getAccessToken());
		assertEquals("raw-refresh-token", result.refreshTokenValue());
		assertEquals("player@example.com", result.response().getUser().getEmail());
		assertEquals("NewPlayer", result.response().getUser().getUsername());

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User captured = userCaptor.getValue();
		assertEquals("player@example.com", captured.getEmail());
		assertEquals("NewPlayer", captured.getUsername());
		assertEquals("encoded-password", captured.getPasswordHash());
		assertNotNull(captured.getCreatedAt());
	}

	@Test
	void registerThrowsConflictWhenEmailAlreadyExists() {
		RegisterRequest request = new RegisterRequest();
		request.setEmail("player@example.com");
		request.setUsername("Taken");
		request.setPassword("password-123");

		when(userRepository.existsByEmail("player@example.com")).thenReturn(true);

		ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.register(request));

		assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
	}

	@Test
	void loginThrowsUnauthorizedForBadPassword() {
		LoginRequest request = new LoginRequest();
		request.setEmail("player@example.com");
		request.setPassword("wrong-password");

		when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(existingUser));
		when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

		ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> authService.login(request));

		assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
	}
}
