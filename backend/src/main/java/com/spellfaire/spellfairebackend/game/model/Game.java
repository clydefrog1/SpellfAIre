package com.spellfaire.spellfairebackend.game.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Represents a game instance between two players (or player vs AI).
 * Contains the complete game state including both players' states.
 */
@Entity
@Table(name = "games")
public class Game {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(nullable = false, length = 36)
	private String player1Id;

	@Column(nullable = false, length = 36)
	private String player2Id;  // Can be "AI" for AI games

	@Column(length = 36)
	private String currentPlayerId;  // Which player's turn it is

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private GameStatus gameStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private GamePhase currentPhase;

	@Column(length = 36)
	private String winnerId;  // null if game is not finished

	@Column(nullable = false)
	private int turnNumber;  // Current turn number (starts at 1)

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "player1_state_id", unique = true)
	private GamePlayerState player1State;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@JoinColumn(name = "player2_state_id", unique = true)
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
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
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
