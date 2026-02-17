package com.spellfaire.spellfairebackend.auth.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spellfaire.spellfairebackend.auth.model.User;

public interface UserRepository extends MongoRepository<User, String> {
	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);
}
