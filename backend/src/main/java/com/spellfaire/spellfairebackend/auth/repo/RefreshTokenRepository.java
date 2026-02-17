package com.spellfaire.spellfairebackend.auth.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spellfaire.spellfairebackend.auth.model.RefreshToken;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
	Optional<RefreshToken> findByTokenHash(String tokenHash);
}
