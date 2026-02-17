package com.spellfaire.spellfairebackend.game.dto;

import java.util.Set;

import com.spellfaire.spellfairebackend.game.model.Keyword;
import com.spellfaire.spellfairebackend.game.model.Status;

/**
 * DTO for a battlefield creature in game responses.
 */
public class BoardCreatureResponse {
	private String instanceId;
	private String cardId;
	private int attack;
	private int health;
	private int maxHealth;
	private boolean canAttack;
	private boolean hasAttackedThisTurn;
	private Set<Keyword> keywords;
	private Set<Status> statuses;
	private int position;

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
