package com.spellfaire.spellfairebackend.game.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a game instance between two players (or player vs AI).
 * Contains the complete game state including both players' states.
 */
@Document("games")
public class Game {
	@Id
	private String id;

	private String player1Id;
	private String player2Id;  // Can be "AI" for AI games

	private String currentPlayerId;  // Which player's turn it is

	private GameStatus gameStatus;
	private GamePhase currentPhase;

	private String winnerId;  // null if game is not finished

	private int turnNumber;  // Current turn number (starts at 1)

	private GamePlayerState player1State;
	private GamePlayerState player2State;

	private Instant createdAt;
	private Instant updatedAt;

	public Game() {
		this.gameStatus = GameStatus.SETUP;
		this.currentPhase = GamePhase.MAIN;
		this.turnNumber = 0;
		this.player1State = new GamePlayerState();
		this.player2State = new GamePlayerState();
	}

	// Getters and setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPlayer1Id() {
		return player1Id;
	}

	public void setPlayer1Id(String player1Id) {
		this.player1Id = player1Id;
	}

	public String getPlayer2Id() {
		return player2Id;
	}

	public void setPlayer2Id(String player2Id) {
		this.player2Id = player2Id;
	}

	public String getCurrentPlayerId() {
		return currentPlayerId;
	}

	public void setCurrentPlayerId(String currentPlayerId) {
		this.currentPlayerId = currentPlayerId;
	}

	public GameStatus getGameStatus() {
		return gameStatus;
	}

	public void setGameStatus(GameStatus gameStatus) {
		this.gameStatus = gameStatus;
	}

	public GamePhase getCurrentPhase() {
		return currentPhase;
	}

	public void setCurrentPhase(GamePhase currentPhase) {
		this.currentPhase = currentPhase;
	}

	public String getWinnerId() {
		return winnerId;
	}

	public void setWinnerId(String winnerId) {
		this.winnerId = winnerId;
	}

	public int getTurnNumber() {
		return turnNumber;
	}

	public void setTurnNumber(int turnNumber) {
		this.turnNumber = turnNumber;
	}

	public GamePlayerState getPlayer1State() {
		return player1State;
	}

	public void setPlayer1State(GamePlayerState player1State) {
		this.player1State = player1State;
	}

	public GamePlayerState getPlayer2State() {
		return player2State;
	}

	public void setPlayer2State(GamePlayerState player2State) {
		this.player2State = player2State;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
