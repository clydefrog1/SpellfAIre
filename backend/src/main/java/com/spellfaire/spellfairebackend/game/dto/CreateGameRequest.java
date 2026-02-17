package com.spellfaire.spellfairebackend.game.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new game.
 */
public class CreateGameRequest {
	@NotBlank(message = "Player 1 deck ID is required")
	private String player1DeckId;

	private String player2Id;  // null for AI game

	private String player2DeckId;  // null for AI game

	// Getters and setters
	public String getPlayer1DeckId() {
		return player1DeckId;
	}

	public void setPlayer1DeckId(String player1DeckId) {
		this.player1DeckId = player1DeckId;
	}

	public String getPlayer2Id() {
		return player2Id;
	}

	public void setPlayer2Id(String player2Id) {
		this.player2Id = player2Id;
	}

	public String getPlayer2DeckId() {
		return player2DeckId;
	}

	public void setPlayer2DeckId(String player2DeckId) {
		this.player2DeckId = player2DeckId;
	}
}
