package com.spellfaire.spellfairebackend.game.model;

/**
 * The four magic schools in SpellfAIre.
 * Each player chooses one school to determine which spells can be included in their deck.
 */
public enum MagicSchool {
	FIRE,     // Direct damage, fast finishes, AoE
	FROST,    // Control, Freeze, tactical removal
	NATURE,   // Healing, buffs, token summoning
	SHADOW    // Drain, debuffs, sacrifice-for-power
}
