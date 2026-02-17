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
import jakarta.persistence.Table;

/**
 * Represents a card in the game (creature or spell).
 * Cards are static game data - they define the 52 cards available in SpellfAIre.
 */
@Entity
@Table(name = "cards")
public class Card {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "BINARY(16)")
	private UUID id;

	@Column(unique = true, nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CardType cardType;

	@Column(nullable = false)
	private int cost;

	// Creature-specific fields
	private Integer attack;        // null for spells
	private Integer health;        // null for spells

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Faction faction;       // null for spells

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "card_keywords", joinColumns = @JoinColumn(name = "card_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "keyword", length = 20)
	private Set<Keyword> keywords; // null or empty for spells

	// Spell-specific fields
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private MagicSchool school;    // null for creatures

	// Common fields
	@Column(columnDefinition = "TEXT")
	private String rulesText;      // Effect description (can be null)

	// Constructors
	public Card() {
	}

	// Getters and setters
	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
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
