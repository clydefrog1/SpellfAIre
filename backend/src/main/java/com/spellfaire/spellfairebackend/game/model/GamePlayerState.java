package com.spellfaire.spellfairebackend.game.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of one player within a game.
 * Embedded within the Game document.
 */
public class GamePlayerState {
	private String userId;
	private String deckId;
	
	// Hero state
	private int heroHealth;       // Starting at 25
	
	// Mana state
	private int maxMana;          // Starting at 1, increases each turn (max 10)
	private int currentMana;      // Refilled to maxMana at start of turn
	
	// Fatigue
	private int fatigueCounter;   // Increments when drawing from empty deck
	
	// Card zones (stored as card IDs)
	private List<String> deck;         // Cards remaining in deck
	private List<String> hand;         // Cards in hand (max 10)
	private List<BoardCreature> battlefield;  // Creatures on the battlefield (max 6)
	private List<String> discardPile;  // Cards that have been played/discarded

	public GamePlayerState() {
		this.heroHealth = 25;
		this.maxMana = 1;
		this.currentMana = 1;
		this.fatigueCounter = 0;
		this.deck = new ArrayList<>();
		this.hand = new ArrayList<>();
		this.battlefield = new ArrayList<>();
		this.discardPile = new ArrayList<>();
	}

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

	public List<BoardCreature> getBattlefield() {
		return battlefield;
	}

	public void setBattlefield(List<BoardCreature> battlefield) {
		this.battlefield = battlefield;
	}

	public List<String> getDiscardPile() {
		return discardPile;
	}

	public void setDiscardPile(List<String> discardPile) {
		this.discardPile = discardPile;
	}
}
