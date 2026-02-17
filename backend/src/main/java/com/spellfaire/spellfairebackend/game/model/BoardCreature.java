package com.spellfaire.spellfairebackend.game.model;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a creature instance on the battlefield.
 * Each instance has its own state (current health, modifications, etc.).
 */
@Entity
@Table(name = "board_creatures")
public class BoardCreature {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;  // was instanceId

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "player_state_id", nullable = false)
	private GamePlayerState playerState;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "card_id", nullable = false)
	private Card card;

	@Column(nullable = false)
	private int attack;         // Can be modified during the game

	@Column(nullable = false)
	private int health;         // Current health

	@Column(nullable = false)
	private int maxHealth;      // Original health (for tracking damage)

	@Column(nullable = false)
	private boolean canAttack;  // False if played this turn without Charge

	@Column(nullable = false)
	private boolean hasAttackedThisTurn;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "board_creature_keywords", joinColumns = @JoinColumn(name = "board_creature_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "keyword", length = 20)
	private Set<Keyword> keywords;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "board_creature_statuses", joinColumns = @JoinColumn(name = "board_creature_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20)
	private Set<Status> statuses;

	@Column(nullable = false)
	private int position;       // 0-5 board position

	public BoardCreature() {
	}

	public BoardCreature(GamePlayerState playerState, Card card, int attack, int health, Set<Keyword> keywords, int position) {
		this.playerState = playerState;
		this.card = card;
		this.attack = attack;
		this.health = health;
		this.maxHealth = health;
		this.canAttack = keywords != null && keywords.contains(Keyword.CHARGE);
		this.hasAttackedThisTurn = false;
		this.keywords = keywords;
		this.position = position;
	}

	// Getters and setters
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

	public int getAttack() {
		return attack;
	}

	public void setAttack(int attack) {
		this.attack = attack;
	}

	public int getHealth() {
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	public void setMaxHealth(int maxHealth) {
		this.maxHealth = maxHealth;
	}

	public boolean isCanAttack() {
		return canAttack;
	}

	public void setCanAttack(boolean canAttack) {
		this.canAttack = canAttack;
	}

	public boolean isHasAttackedThisTurn() {
		return hasAttackedThisTurn;
	}

	public void setHasAttackedThisTurn(boolean hasAttackedThisTurn) {
		this.hasAttackedThisTurn = hasAttackedThisTurn;
	}

	public Set<Keyword> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<Keyword> keywords) {
		this.keywords = keywords;
	}

	public Set<Status> getStatuses() {
		return statuses;
	}

	public void setStatuses(Set<Status> statuses) {
		this.statuses = statuses;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
}
