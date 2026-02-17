package com.spellfaire.spellfairebackend.game.dto;

import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new game against the AI.
 */
public class CreateAiGameRequest {

	@NotNull(message = "Faction is required")
	private Faction faction;

	@NotNull(message = "Magic school is required")
	private MagicSchool magicSchool;

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
}
