package com.spellfaire.spellfairebackend.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Represents the state of one player within a game.
 * Stored as a separate table, referenced by the Game entity.
 */
@Entity
@Table(name = "game_player_states")
public class GamePlayerState {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(length = 36)
	private String userId;

	@Column(length = 36)
	private String deckId;

	// Hero state
	@Column(nullable = false)
	private int heroHealth;       // Starting at 25

	// Mana state
	@Column(nullable = false)
	private int maxMana;          // Starting at 1, increases each turn (max 10)

	@Column(nullable = false)
	private int currentMana;      // Refilled to maxMana at start of turn

	// Fatigue
	@Column(nullable = false)
	private int fatigueCounter;   // Increments when drawing from empty deck

	// Card zones (normalized into separate tables)
	@OneToMany(mappedBy = "playerState", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("position ASC")
	private List<PlayerZoneCard> zoneCards = new ArrayList<>();

	@OneToMany(mappedBy = "playerState", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("position ASC")
	private List<BoardCreature> battlefield = new ArrayList<>();

	public GamePlayerState() {
		this.heroHealth = 25;
		this.maxMana = 1;
		this.currentMana = 1;
		this.fatigueCounter = 0;
	}

	// Getters and setters
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

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

	public List<PlayerZoneCard> getZoneCards() {
		return zoneCards;
	}

	public void setZoneCards(List<PlayerZoneCard> zoneCards) {
		this.zoneCards = zoneCards;
	}

	public List<BoardCreature> getBattlefield() {
		return battlefield;
	}

	public void setBattlefield(List<BoardCreature> battlefield) {
		this.battlefield = battlefield;
	}

	// Convenience methods for card zones
	public List<String> getDeckCardIds() {
		return zoneCards.stream()
			.filter(c -> c.getZone() == CardZone.DECK)
			.map(c -> c.getCard().getId().toString())
			.toList();
	}

	public List<String> getHandCardIds() {
		return zoneCards.stream()
			.filter(c -> c.getZone() == CardZone.HAND)
			.map(c -> c.getCard().getId().toString())
			.toList();
	}

	public List<String> getDiscardPileCardIds() {
		return zoneCards.stream()
			.filter(c -> c.getZone() == CardZone.DISCARD)
			.map(c -> c.getCard().getId().toString())
			.toList();
	}
}
