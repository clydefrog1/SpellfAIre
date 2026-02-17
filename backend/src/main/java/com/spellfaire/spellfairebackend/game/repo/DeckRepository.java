package com.spellfaire.spellfairebackend.game.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spellfaire.spellfairebackend.game.model.Deck;

/**
 * Repository for Deck entities.
 */
public interface DeckRepository extends MongoRepository<Deck, String> {
	
	List<Deck> findByUserId(String userId);
	
	List<Deck> findByUserIdOrderByUpdatedAtDesc(String userId);
}
