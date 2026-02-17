package com.spellfaire.spellfairebackend.game.model;

/**
 * Represents a single card entry in a deck (card reference + quantity).
 */
public class DeckCard {
	private String cardId;
	private int quantity; // 1 or 2 (max 2 copies per card)

	public DeckCard() {
	}

	public DeckCard(String cardId, int quantity) {
		this.cardId = cardId;
		this.quantity = quantity;
	}

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
