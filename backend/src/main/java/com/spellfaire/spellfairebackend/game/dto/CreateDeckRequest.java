package com.spellfaire.spellfairebackend.game.dto;

import java.util.List;

import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new deck.
 */
public class CreateDeckRequest {
	@NotBlank(message = "Deck name is required")
	private String name;

	@NotNull(message = "Faction is required")
	private Faction faction;

	@NotNull(message = "Magic school is required")
	private MagicSchool magicSchool;

	@NotEmpty(message = "Cards list cannot be empty")
	@Valid
	private List<DeckCardRequest> cards;

	// Getters and setters
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

	public List<DeckCardRequest> getCards() {
		return cards;
	}

	public void setCards(List<DeckCardRequest> cards) {
		this.cards = cards;
	}
}
