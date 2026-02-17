package com.spellfaire.spellfairebackend.game.model;

/**
 * Represents which zone a card is in during a game.
 */
public enum CardZone {
	DECK,     // Cards remaining in the draw pile
	HAND,     // Cards in the player's hand
	DISCARD   // Cards that have been played/discarded
}
