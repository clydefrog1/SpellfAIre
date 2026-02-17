package com.spellfaire.spellfairebackend.game.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a player's deck.
 * A deck must contain exactly 24 cards: 14 creatures from one faction and 10 spells from one school.
 * Up to 2 copies of any card are allowed.
 */
@Document("decks")
public class Deck {
	@Id
	private String id;

	private String userId;

	private String name;

	private Faction faction;

	private MagicSchool magicSchool;

	private List<DeckCard> cards;

	private Instant createdAt;

	private Instant updatedAt;

	public Deck() {
	}

	// Getters and setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Faction getFaction() {
		return faction;
	}

	public void setFaction(Faction faction) {
		this.faction = faction;
	}

	public MagicSchool getMagicSchool() {
		return magicSchool;
	}

	public void setMagicSchool(MagicSchool magicSchool) {
		this.magicSchool = magicSchool;
	}

	public List<DeckCard> getCards() {
		return cards;
	}

	public void setCards(List<DeckCard> cards) {
		this.cards = cards;
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
