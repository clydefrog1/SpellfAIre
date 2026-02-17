package com.spellfaire.spellfairebackend.game.model;

/**
 * Current phase within a turn.
 */
public enum GamePhase {
	START,  // Ready creatures, start-of-turn triggers
	DRAW,   // Draw a card
	MAIN,   // Play cards, attack
	END     // End-of-turn triggers
}
