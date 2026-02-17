package com.spellfaire.spellfairebackend.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for a card entry in a deck.
 */
public class DeckCardRequest {
	@NotBlank(message = "Card ID is required")
	private String cardId;

	@Min(value = 1, message = "Quantity must be at least 1")
	@Max(value = 2, message = "Maximum 2 copies of any card allowed")
	private int quantity;

	// Getters and setters
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
