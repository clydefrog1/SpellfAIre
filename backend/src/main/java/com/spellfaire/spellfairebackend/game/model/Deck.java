package com.spellfaire.spellfairebackend.game.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Represents a player's deck.
 * A deck must contain exactly 24 cards: 14 creatures from one faction and 10 spells from one school.
 * Up to 2 copies of any card are allowed.
 */
@Entity
@Table(name = "decks")
public class Deck {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private com.spellfaire.spellfairebackend.auth.model.User user;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Faction faction;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MagicSchool magicSchool;

	@OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<DeckCard> deckCards = new ArrayList<>();

	private Instant createdAt;

	private Instant updatedAt;

	public Deck() {
	}

	// Getters and setters
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public com.spellfaire.spellfairebackend.auth.model.User getUser() {
		return user;
	}

	public void setUser(com.spellfaire.spellfairebackend.auth.model.User user) {
		this.user = user;
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

	public List<DeckCard> getDeckCards() {
		return deckCards;
	}

	public void setDeckCards(List<DeckCard> deckCards) {
		this.deckCards = deckCards;
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
