package com.spellfaire.spellfairebackend.game.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.UserRepository;
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
	private final UserRepository userRepository;

	public GameController(GameService gameService, UserRepository userRepository) {
		this.gameService = gameService;
		this.userRepository = userRepository;
	}

	/**
	 * Create a new game.
	 */
	@PostMapping
	public ResponseEntity<GameResponse> createGame(
		@AuthenticationPrincipal UserDetails userDetails,
		@Valid @RequestBody CreateGameRequest request
	) {
		User user = userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new IllegalStateException("User not found"));
		
		try {
			GameResponse response = gameService.createGame(user.getId(), request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException | UnsupportedOperationException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	/**
	 * Get all games for the authenticated user.
	 */
	@GetMapping
	public ResponseEntity<List<GameResponse>> getPlayerGames(
		@AuthenticationPrincipal UserDetails userDetails
	) {
		User user = userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new IllegalStateException("User not found"));
		
		List<GameResponse> games = gameService.getPlayerGames(user.getId());
		return ResponseEntity.ok(games);
	}

	/**
	 * Get active games for the authenticated user.
	 */
	@GetMapping("/active")
	public ResponseEntity<List<GameResponse>> getActiveGames(
		@AuthenticationPrincipal UserDetails userDetails
	) {
		User user = userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new IllegalStateException("User not found"));
		
		List<GameResponse> games = gameService.getActiveGames(user.getId());
		return ResponseEntity.ok(games);
	}

	/**
	 * Get a specific game by ID.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<GameResponse> getGameById(@PathVariable String id) {
		return gameService.getGameById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}
}
