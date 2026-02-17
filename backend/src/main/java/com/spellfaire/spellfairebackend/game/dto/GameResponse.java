package com.spellfaire.spellfairebackend.game.dto;

import java.time.Instant;

import com.spellfaire.spellfairebackend.game.model.GamePhase;
import com.spellfaire.spellfairebackend.game.model.GameStatus;

/**
 * DTO for Game responses.
 */
public class GameResponse {
	private String id;
	private String player1Id;
	private String player2Id;
	private String currentPlayerId;
	private GameStatus gameStatus;
	private GamePhase currentPhase;
	private String winnerId;
	private int turnNumber;
	private GamePlayerStateResponse player1State;
	private GamePlayerStateResponse player2State;
	private Instant createdAt;
	private Instant updatedAt;

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

	public GamePlayerStateResponse getPlayer1State() {
		return player1State;
	}

	public void setPlayer1State(GamePlayerStateResponse player1State) {
		this.player1State = player1State;
	}

	public GamePlayerStateResponse getPlayer2State() {
		return player2State;
	}

	public void setPlayer2State(GamePlayerStateResponse player2State) {
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
