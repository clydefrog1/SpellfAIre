package com.spellfaire.spellfairebackend.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.spellfaire.spellfairebackend.auth.model.RefreshToken;
import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.RefreshTokenRepository;

@Service
public class RefreshTokenService {
	private final RefreshTokenRepository refreshTokenRepository;
	private final SecureRandom secureRandom = new SecureRandom();
	private final long refreshTtlSeconds;

	public RefreshTokenService(
			RefreshTokenRepository refreshTokenRepository,
			@Value("${spellfaire.security.refresh.ttl-seconds}") long refreshTtlSeconds) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.refreshTtlSeconds = refreshTtlSeconds;
	}

	public IssuedRefreshToken issueForUser(User user) {
		String rawToken = generateTokenValue();
		String tokenHash = sha256Base64Url(rawToken);

		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setUser(user);
		refreshToken.setTokenHash(tokenHash);
		refreshToken.setCreatedAt(Instant.now());
		refreshToken.setExpiresAt(Instant.now().plusSeconds(refreshTtlSeconds));

		refreshTokenRepository.save(refreshToken);

		return new IssuedRefreshToken(rawToken, refreshToken);
	}

	public IssuedRefreshToken rotate(String rawToken) {
		String tokenHash = sha256Base64Url(rawToken);
		RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

		if (existing.isRevoked()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked");
		}
		if (existing.getExpiresAt() != null && existing.getExpiresAt().isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
		}

		IssuedRefreshToken issued = issueForUser(existing.getUser());
		existing.setRevokedAt(Instant.now());
		existing.setReplacedByTokenHash(issued.persisted().getTokenHash());
		refreshTokenRepository.save(existing);

		return issued;
	}

	public void revokeIfPresent(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}

		String tokenHash = sha256Base64Url(rawToken);
		refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
			if (!token.isRevoked()) {
				token.setRevokedAt(Instant.now());
				refreshTokenRepository.save(token);
			}
		});
	}

	private String generateTokenValue() {
		byte[] bytes = new byte[48];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String sha256Base64Url(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	public record IssuedRefreshToken(String rawToken, RefreshToken persisted) {
	}
}
