package com.spellfaire.spellfairebackend.game.dto;

import java.time.Instant;
import java.util.List;

import com.spellfaire.spellfairebackend.game.model.DeckCard;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;

/**
 * DTO for Deck responses.
 */
public class DeckResponse {
	private String id;
	private String userId;
	private String name;
	private Faction faction;
	private MagicSchool magicSchool;
	private List<DeckCard> cards;
	private Instant createdAt;
	private Instant updatedAt;

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
