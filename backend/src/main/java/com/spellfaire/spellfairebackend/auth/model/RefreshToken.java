package com.spellfaire.spellfairebackend.auth.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("refresh_tokens")
public class RefreshToken {
	@Id
	private String id;

	@Indexed
	private String userId;

	@Indexed(unique = true)
	private String tokenHash;

	private Instant createdAt;

	private Instant expiresAt;

	private Instant revokedAt;

	private String replacedByTokenHash;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	public String getReplacedByTokenHash() {
		return replacedByTokenHash;
	}

	public void setReplacedByTokenHash(String replacedByTokenHash) {
		this.replacedByTokenHash = replacedByTokenHash;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}
}
