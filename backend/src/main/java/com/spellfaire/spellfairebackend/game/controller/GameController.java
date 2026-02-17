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

import com.spellfaire.spellfairebackend.game.dto.CreateGameRequest;
import com.spellfaire.spellfairebackend.game.dto.GameResponse;
import com.spellfaire.spellfairebackend.game.service.GameService;

import jakarta.validation.Valid;

/**
 * REST controller for game management.
 * Handles game creation and state retrieval.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

	private final GameService gameService;

	public GameController(GameService gameService) {
		this.gameService = gameService;
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
}
