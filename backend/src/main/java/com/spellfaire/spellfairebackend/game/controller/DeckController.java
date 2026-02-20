package com.spellfaire.spellfairebackend.game.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spellfaire.spellfairebackend.game.dto.CreateDeckRequest;
import com.spellfaire.spellfairebackend.game.dto.DeckResponse;
import com.spellfaire.spellfairebackend.game.service.DeckService;

import jakarta.validation.Valid;

/**
 * REST controller for deck management.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/decks")
public class DeckController {

	private final DeckService deckService;

	public DeckController(DeckService deckService) {
		this.deckService = deckService;
	}

	private UUID currentUserId(Authentication authentication) {
		return UUID.fromString((String) authentication.getPrincipal());
	}

	/**
	 * Create a new deck for the authenticated user.
	 */
	@PostMapping
	public ResponseEntity<DeckResponse> createDeck(
		Authentication authentication,
		@Valid @RequestBody CreateDeckRequest request
	) {
		DeckResponse response = deckService.createDeck(currentUserId(authentication), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Get all decks for the authenticated user.
	 */
	@GetMapping
	public ResponseEntity<List<DeckResponse>> getUserDecks(Authentication authentication) {
		List<DeckResponse> decks = deckService.getUserDecks(currentUserId(authentication));
		return ResponseEntity.ok(decks);
	}

	/**
	 * Get a specific deck by ID.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<DeckResponse> getDeckById(@PathVariable String id) {
		return deckService.getDeckById(UUID.fromString(id))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Update an existing deck.
	 */
	@PutMapping("/{id}")
	public ResponseEntity<DeckResponse> updateDeck(
		@PathVariable String id,
		Authentication authentication,
		@Valid @RequestBody CreateDeckRequest request
	) {
		return deckService.updateDeck(UUID.fromString(id), currentUserId(authentication), request)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Delete a deck.
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDeck(
		@PathVariable String id,
		Authentication authentication
	) {
		boolean deleted = deckService.deleteDeck(UUID.fromString(id), currentUserId(authentication));
		return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
