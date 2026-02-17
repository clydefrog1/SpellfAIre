package com.spellfaire.spellfairebackend.game.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.UserRepository;
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
	private final UserRepository userRepository;

	public DeckController(DeckService deckService, UserRepository userRepository) {
		this.deckService = deckService;
		this.userRepository = userRepository;
	}

	/**
	 * Create a new deck for the authenticated user.
	 */
	@PostMapping
	public ResponseEntity<DeckResponse> createDeck(
		@AuthenticationPrincipal UserDetails userDetails,
		@Valid @RequestBody CreateDeckRequest request
	) {
		User user = userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new IllegalStateException("User not found"));
		
		try {
			DeckResponse response = deckService.createDeck(user.getId(), request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Get all decks for the authenticated user.
	 */
	@GetMapping
	public ResponseEntity<List<DeckResponse>> getUserDecks(
		@AuthenticationPrincipal UserDetails userDetails
	) {
		User user = userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new IllegalStateException("User not found"));
		
		List<DeckResponse> decks = deckService.getUserDecks(user.getId());
		return ResponseEntity.ok(decks);
	}

	/**
	 * Get a specific deck by ID.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<DeckResponse> getDeckById(@PathVariable String id) {
		return deckService.getDeckById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Update an existing deck.
	 */
	@PutMapping("/{id}")
	public ResponseEntity<DeckResponse> updateDeck(
		@PathVariable String id,
		@AuthenticationPrincipal UserDetails userDetails,
		@Valid @RequestBody CreateDeckRequest request
	) {
		User user = userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new IllegalStateException("User not found"));
		
		try {
			return deckService.updateDeck(id, user.getId(), request)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Delete a deck.
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDeck(
		@PathVariable String id,
		@AuthenticationPrincipal UserDetails userDetails
	) {
		User user = userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new IllegalStateException("User not found"));
		
		boolean deleted = deckService.deleteDeck(id, user.getId());
		return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}
