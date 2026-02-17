package com.spellfaire.spellfairebackend.game.dto;

/**
 * DTO for playing a card from hand.
 */
public class PlayCardRequest {

	private String cardId;

	/**
	 * Optional target for targeted spells/battlecries.
	 * Values: creature instanceId, "ENEMY_HERO", or "FRIENDLY_HERO".
	 */
	private String targetId;

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}
}
