package com.spellfaire.spellfairebackend.game.model;

/**
 * Keywords that can appear on creatures.
 */
public enum Keyword {
	GUARD,      // Enemies must target Guard creatures first with attacks
	CHARGE,     // Can attack the turn it is played
	LIFESTEAL,  // Damage dealt heals your Hero for the same amount
	WARD        // First damage is prevented and Ward is removed
}
