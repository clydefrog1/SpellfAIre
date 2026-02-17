package com.spellfaire.spellfairebackend.game.model;

/**
 * Overall state of a game.
 */
public enum GameStatus {
	SETUP,        // Game is being set up (mulligan, etc.)
	IN_PROGRESS,  // Game is actively being played
	FINISHED      // Game has ended
}
