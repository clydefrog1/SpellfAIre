package com.spellfaire.spellfairebackend.game.model;

/**
 * The four creature factions in SpellfAIre.
 * Each player chooses one faction to determine which creatures can be included in their deck.
 */
public enum Faction {
	KINGDOM,      // Order - defensive, Guard, buffs
	WILDCLAN,     // Beasts - aggressive, Charge, swarming
	NECROPOLIS,   // Undead - death triggers, value, recursion
	IRONBOUND     // Constructs - Ward, scaling, durable
}
