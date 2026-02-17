package com.spellfaire.spellfairebackend.game.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a card in a specific zone during a game (deck, hand, or discard pile).
 * Normalizes the card lists that were previously embedded arrays in GamePlayerState.
 */
@Entity
@Table(name = "player_zone_cards", indexes = {
	@Index(name = "idx_pzc_state_zone", columnList = "player_state_id, zone, position")
})
public class PlayerZoneCard {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_state_id", nullable = false)
	private GamePlayerState playerState;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "card_id", nullable = false)
	private Card card;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private CardZone zone;

	@Column(nullable = false)
	private int position;  // Order within the zone

	public PlayerZoneCard() {
	}

	public PlayerZoneCard(GamePlayerState playerState, Card card, CardZone zone, int position) {
		this.playerState = playerState;
		this.card = card;
		this.zone = zone;
		this.position = position;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public GamePlayerState getPlayerState() {
		return playerState;
	}

	public void setPlayerState(GamePlayerState playerState) {
		this.playerState = playerState;
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}

	public CardZone getZone() {
		return zone;
	}

	public void setZone(CardZone zone) {
		this.zone = zone;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
}
