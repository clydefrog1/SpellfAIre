package com.spellfaire.spellfairebackend.game.dto;

/**
 * Represents a single event that occurred during game action resolution.
 * Used to communicate what happened to the frontend for animations and logging.
 */
public class GameEvent {

	public enum EventType {
		DAMAGE, HEAL, DEATH, CARD_PLAYED, CARD_DRAWN, SPELL_RESOLVED,
		ATTACK, FATIGUE, BUFF, FREEZE, GAME_OVER, SUMMON, TURN_START, MANA_GAIN
	}

	private EventType type;
	private String sourceId;
	private String targetId;
	private int value;
	private String message;

	public GameEvent() {
	}

	public GameEvent(EventType type, String sourceId, String targetId, int value, String message) {
		this.type = type;
		this.sourceId = sourceId;
		this.targetId = targetId;
		this.value = value;
		this.message = message;
	}

	public static GameEvent damage(String sourceId, String targetId, int amount, String message) {
		return new GameEvent(EventType.DAMAGE, sourceId, targetId, amount, message);
	}

	public static GameEvent heal(String sourceId, String targetId, int amount, String message) {
		return new GameEvent(EventType.HEAL, sourceId, targetId, amount, message);
	}

	public static GameEvent death(String creatureId, String message) {
		return new GameEvent(EventType.DEATH, creatureId, null, 0, message);
	}

	public static GameEvent cardPlayed(String cardId, String message) {
		return new GameEvent(EventType.CARD_PLAYED, cardId, null, 0, message);
	}

	public static GameEvent cardDrawn(String playerId, String message) {
		return new GameEvent(EventType.CARD_DRAWN, playerId, null, 0, message);
	}

	public static GameEvent spellResolved(String cardId, String message) {
		return new GameEvent(EventType.SPELL_RESOLVED, cardId, null, 0, message);
	}

	public static GameEvent attack(String attackerId, String targetId, String message) {
		return new GameEvent(EventType.ATTACK, attackerId, targetId, 0, message);
	}

	public static GameEvent fatigue(String playerId, int damage, String message) {
		return new GameEvent(EventType.FATIGUE, playerId, null, damage, message);
	}

	public static GameEvent buff(String targetId, int attackBuff, String message) {
		return new GameEvent(EventType.BUFF, null, targetId, attackBuff, message);
	}

	public static GameEvent freeze(String targetId, String message) {
		return new GameEvent(EventType.FREEZE, null, targetId, 0, message);
	}

	public static GameEvent gameOver(String winnerId, String message) {
		return new GameEvent(EventType.GAME_OVER, winnerId, null, 0, message);
	}

	public static GameEvent summon(String creatureId, String message) {
		return new GameEvent(EventType.SUMMON, creatureId, null, 0, message);
	}

	public static GameEvent turnStart(String playerId, int turnNumber, String message) {
		return new GameEvent(EventType.TURN_START, playerId, null, turnNumber, message);
	}

	public static GameEvent manaGain(String playerId, int maxMana, String message) {
		return new GameEvent(EventType.MANA_GAIN, playerId, null, maxMana, message);
	}

	// Getters and setters
	public EventType getType() { return type; }
	public void setType(EventType type) { this.type = type; }

	public String getSourceId() { return sourceId; }
	public void setSourceId(String sourceId) { this.sourceId = sourceId; }

	public String getTargetId() { return targetId; }
	public void setTargetId(String targetId) { this.targetId = targetId; }

	public int getValue() { return value; }
	public void setValue(int value) { this.value = value; }

	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
}
