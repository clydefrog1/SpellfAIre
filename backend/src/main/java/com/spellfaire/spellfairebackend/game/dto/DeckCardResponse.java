package com.spellfaire.spellfairebackend.game.dto;

/**
 * DTO for a card entry in a deck response.
 */
public class DeckCardResponse {
	private String cardId;
	private int quantity;

	public DeckCardResponse() {
	}

	public DeckCardResponse(String cardId, int quantity) {
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
