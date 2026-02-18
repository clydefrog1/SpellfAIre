package com.spellfaire.spellfairebackend.game.dto;

import java.util.Set;

import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.Keyword;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;

/**
 * DTO for Card responses.
 */
public class CardResponse {
	private String id;
	private String name;
	private CardType cardType;
	private int cost;
	private Integer attack;
	private Integer health;
	private Faction faction;
	private Set<Keyword> keywords;
	private MagicSchool school;
	private String rulesText;
	private String flavorText;

	// Getters and setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CardType getCardType() {
		return cardType;
	}

	public void setCardType(CardType cardType) {
		this.cardType = cardType;
	}

	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public Integer getAttack() {
		return attack;
	}

	public void setAttack(Integer attack) {
		this.attack = attack;
	}

	public Integer getHealth() {
		return health;
	}

	public void setHealth(Integer health) {
		this.health = health;
	}

	public Faction getFaction() {
		return faction;
	}

	public void setFaction(Faction faction) {
		this.faction = faction;
	}

	public Set<Keyword> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<Keyword> keywords) {
		this.keywords = keywords;
	}

	public MagicSchool getSchool() {
		return school;
	}

	public void setSchool(MagicSchool school) {
		this.school = school;
	}

	public String getRulesText() {
		return rulesText;
	}

	public void setRulesText(String rulesText) {
		this.rulesText = rulesText;
	}

	public String getFlavorText() {
		return flavorText;
	}

	public void setFlavorText(String flavorText) {
		this.flavorText = flavorText;
	}
}
