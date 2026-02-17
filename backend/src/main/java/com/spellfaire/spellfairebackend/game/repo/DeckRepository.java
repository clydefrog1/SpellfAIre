package com.spellfaire.spellfairebackend.game.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spellfaire.spellfairebackend.game.model.Deck;

/**
 * Repository for Deck entities.
 */
public interface DeckRepository extends JpaRepository<Deck, UUID> {

	List<Deck> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
