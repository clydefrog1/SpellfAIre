package com.spellfaire.spellfairebackend.game.dto;

import java.util.List;

/**
 * DTO for player state in game responses.
 * Mirrors the old GamePlayerState structure for API backward compatibility.
 */
public class GamePlayerStateResponse {
	private String userId;
	private String deckId;
	private int heroHealth;
	private int maxMana;
	private int currentMana;
	private int fatigueCounter;
	private List<String> deck;
	private List<String> hand;
	private List<BoardCreatureResponse> battlefield;
	private List<String> discardPile;

	// Getters and setters
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDeckId() {
		return deckId;
	}

	public void setDeckId(String deckId) {
		this.deckId = deckId;
	}

	public int getHeroHealth() {
		return heroHealth;
	}

	public void setHeroHealth(int heroHealth) {
		this.heroHealth = heroHealth;
	}

	public int getMaxMana() {
		return maxMana;
	}

	public void setMaxMana(int maxMana) {
		this.maxMana = maxMana;
	}

	public int getCurrentMana() {
		return currentMana;
	}

	public void setCurrentMana(int currentMana) {
		this.currentMana = currentMana;
	}

	public int getFatigueCounter() {
		return fatigueCounter;
	}

	public void setFatigueCounter(int fatigueCounter) {
		this.fatigueCounter = fatigueCounter;
	}

	public List<String> getDeck() {
		return deck;
	}

	public void setDeck(List<String> deck) {
		this.deck = deck;
	}

	public List<String> getHand() {
		return hand;
	}

	public void setHand(List<String> hand) {
		this.hand = hand;
	}

	public List<BoardCreatureResponse> getBattlefield() {
		return battlefield;
	}

	public void setBattlefield(List<BoardCreatureResponse> battlefield) {
		this.battlefield = battlefield;
	}

	public List<String> getDiscardPile() {
		return discardPile;
	}

	public void setDiscardPile(List<String> discardPile) {
		this.discardPile = discardPile;
	}
}
