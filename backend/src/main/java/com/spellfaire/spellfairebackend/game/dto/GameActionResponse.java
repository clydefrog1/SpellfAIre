package com.spellfaire.spellfairebackend.game.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for game actions.
 * Contains the updated game state plus a list of events that occurred.
 */
public class GameActionResponse {

	private GameResponse game;
	private List<GameEvent> events;

	public GameActionResponse() {
		this.events = new ArrayList<>();
	}

	public GameActionResponse(GameResponse game, List<GameEvent> events) {
		this.game = game;
		this.events = events != null ? events : new ArrayList<>();
	}

	public GameResponse getGame() {
		return game;
	}

	public void setGame(GameResponse game) {
		this.game = game;
	}

	public List<GameEvent> getEvents() {
		return events;
	}

	public void setEvents(List<GameEvent> events) {
		this.events = events;
	}
}
