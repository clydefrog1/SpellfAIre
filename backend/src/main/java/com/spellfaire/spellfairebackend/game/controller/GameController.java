package com.spellfaire.spellfairebackend.game.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spellfaire.spellfairebackend.game.dto.AttackRequest;
import com.spellfaire.spellfairebackend.game.dto.CreateAiGameRequest;
import com.spellfaire.spellfairebackend.game.dto.CreateGameRequest;
import com.spellfaire.spellfairebackend.game.dto.GameActionResponse;
import com.spellfaire.spellfairebackend.game.dto.GameResponse;
import com.spellfaire.spellfairebackend.game.dto.PlayCardRequest;
import com.spellfaire.spellfairebackend.game.service.GameService;
import com.spellfaire.spellfairebackend.game.service.GameplayService;

import jakarta.validation.Valid;

/**
 * REST controller for game management.
 * Handles game creation, state retrieval, and gameplay actions.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

	private final GameService gameService;
	private final GameplayService gameplayService;

	public GameController(GameService gameService, GameplayService gameplayService) {
		this.gameService = gameService;
		this.gameplayService = gameplayService;
	}

	private String currentUserId(Authentication authentication) {
		return (String) authentication.getPrincipal();
	}

	/**
	 * Create a new game.
	 */
	@PostMapping
	public ResponseEntity<GameResponse> createGame(
		Authentication authentication,
		@Valid @RequestBody CreateGameRequest request
	) {
		try {
			GameResponse response = gameService.createGame(currentUserId(authentication), request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException | UnsupportedOperationException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Get all games for the authenticated user.
	 */
	@GetMapping
	public ResponseEntity<List<GameResponse>> getPlayerGames(Authentication authentication) {
		List<GameResponse> games = gameService.getPlayerGames(currentUserId(authentication));
		return ResponseEntity.ok(games);
	}

	/**
	 * Get active games for the authenticated user.
	 */
	@GetMapping("/active")
	public ResponseEntity<List<GameResponse>> getActiveGames(Authentication authentication) {
		List<GameResponse> games = gameService.getActiveGames(currentUserId(authentication));
		return ResponseEntity.ok(games);
	}

	/**
	 * Get a specific game by ID.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<GameResponse> getGameById(@PathVariable String id) {
		return gameService.getGameById(UUID.fromString(id))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	// ==================================================================
	// GAMEPLAY ENDPOINTS
	// ==================================================================

	/**
	 * Create a new game against the AI.
	 */
	@PostMapping("/ai")
	public ResponseEntity<GameActionResponse> createAiGame(
		Authentication authentication,
		@Valid @RequestBody CreateAiGameRequest request
	) {
		try {
			GameActionResponse response = gameplayService.createAiGame(currentUserId(authentication), request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Play a card from hand.
	 */
	@PostMapping("/{id}/play-card")
	public ResponseEntity<GameActionResponse> playCard(
		Authentication authentication,
		@PathVariable String id,
		@Valid @RequestBody PlayCardRequest request
	) {
		try {
			GameActionResponse response = gameplayService.playCard(UUID.fromString(id), currentUserId(authentication), request);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException | IllegalStateException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Attack with a creature.
	 */
	@PostMapping("/{id}/attack")
	public ResponseEntity<GameActionResponse> attack(
		Authentication authentication,
		@PathVariable String id,
		@Valid @RequestBody AttackRequest request
	) {
		try {
			GameActionResponse response = gameplayService.attack(UUID.fromString(id), currentUserId(authentication), request);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException | IllegalStateException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * End the current player's turn.
	 */
	@PostMapping("/{id}/end-turn")
	public ResponseEntity<GameActionResponse> endTurn(
		Authentication authentication,
		@PathVariable String id
	) {
		try {
			GameActionResponse response = gameplayService.endTurn(UUID.fromString(id), currentUserId(authentication));
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException | IllegalStateException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Surrender / concede the game.
	 */
	@PostMapping("/{id}/surrender")
	public ResponseEntity<GameActionResponse> surrender(
		Authentication authentication,
		@PathVariable String id
	) {
		try {
			GameActionResponse response = gameplayService.surrender(UUID.fromString(id), currentUserId(authentication));
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}
}
