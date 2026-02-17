package com.spellfaire.spellfairebackend.game.model;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a card in the game (creature or spell).
 * Cards are static game data - they define the 52 cards available in SpellfAIre.
 */
@Document("cards")
public class Card {
	@Id
	private String id;

	@Indexed(unique = true)
	private String name;

	private CardType cardType;

	private int cost;

	// Creature-specific fields
	private Integer attack;        // null for spells
	private Integer health;        // null for spells
	private Faction faction;       // null for spells
	private Set<Keyword> keywords; // null or empty for spells

	// Spell-specific fields
	private MagicSchool school;    // null for creatures

	// Common fields
	private String rulesText;      // Effect description (can be null)

	// Constructors
	public Card() {
	}

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
}
