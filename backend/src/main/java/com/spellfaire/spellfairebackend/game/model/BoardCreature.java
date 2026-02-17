package com.spellfaire.spellfairebackend.game.model;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a creature instance on the battlefield.
 * Each instance has its own state (current health, modifications, etc.).
 */
public class BoardCreature {
	private String instanceId;  // Unique ID for this battlefield instance
	private String cardId;      // Reference to the Card definition
	private int attack;         // Can be modified during the game
	private int health;         // Current health
	private int maxHealth;      // Original health (for tracking damage)
	private boolean canAttack;  // False if played this turn without Charge
	private boolean hasAttackedThisTurn;
	private Set<Keyword> keywords;
	private Set<Status> statuses;
	private int position;       // 0-5 board position

	public BoardCreature() {
		this.instanceId = UUID.randomUUID().toString();
	}

	public BoardCreature(String cardId, int attack, int health, Set<Keyword> keywords, int position) {
		this.instanceId = UUID.randomUUID().toString();
		this.cardId = cardId;
		this.attack = attack;
		this.health = health;
		this.maxHealth = health;
		this.canAttack = keywords != null && keywords.contains(Keyword.CHARGE);
		this.hasAttackedThisTurn = false;
		this.keywords = keywords;
		this.position = position;
	}

	// Getters and setters
	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
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
