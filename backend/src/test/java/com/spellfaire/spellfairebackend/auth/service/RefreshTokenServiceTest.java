package com.spellfaire.spellfairebackend.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.spellfaire.spellfairebackend.auth.model.RefreshToken;
import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	private RefreshTokenService refreshTokenService;

	private User user;

	@BeforeEach
	void setUp() {
		refreshTokenService = new RefreshTokenService(refreshTokenRepository, 3600);

		user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("refresh@example.com");
		user.setUsername("RefreshUser");
	}

	@Test
	void issueForUserCreatesPersistedTokenAndRawValue() {
		when(refreshTokenRepository.save(any(RefreshToken.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issueForUser(user);

		assertNotNull(issued.rawToken());
		assertNotNull(issued.persisted());
		assertEquals(user, issued.persisted().getUser());
		assertNotNull(issued.persisted().getTokenHash());
		assertNotNull(issued.persisted().getCreatedAt());
		assertNotNull(issued.persisted().getExpiresAt());
		assertEquals(43, issued.persisted().getTokenHash().length());
		assertNotEquals(issued.rawToken(), issued.persisted().getTokenHash());
	}

	@Test
	void rotateRejectsUnknownToken() {
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> refreshTokenService.rotate("unknown-token"));
	}

	@Test
	void rotateRejectsRevokedToken() {
		RefreshToken existing = tokenForUser(user);
		existing.setRevokedAt(Instant.now().minusSeconds(10));
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));

		assertThrows(ResponseStatusException.class, () -> refreshTokenService.rotate("revoked-token"));
	}

	@Test
	void rotateRejectsExpiredToken() {
		RefreshToken existing = tokenForUser(user);
		existing.setExpiresAt(Instant.now().minusSeconds(1));
		when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));

		assertThrows(ResponseStatusException.class, () -> refreshTokenService.rotate("expired-token"));
	}

	@Test
	void revokeIfPresentSkipsBlankToken() {
		refreshTokenService.revokeIfPresent("  ");

		verify(refreshTokenRepository, never()).findByTokenHash(any());
	}

	private static RefreshToken tokenForUser(User user) {
		RefreshToken token = new RefreshToken();
		token.setUser(user);
		token.setTokenHash("a".repeat(64));
		token.setCreatedAt(Instant.now().minusSeconds(60));
		token.setExpiresAt(Instant.now().plusSeconds(60));
		return token;
	}
}
