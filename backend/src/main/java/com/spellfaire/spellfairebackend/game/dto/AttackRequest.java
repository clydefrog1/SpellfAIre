package com.spellfaire.spellfairebackend.game.dto;

/**
 * DTO for attacking with a creature.
 */
public class AttackRequest {

	private String attackerInstanceId;

	/**
	 * Target of the attack: creature instanceId or "ENEMY_HERO".
	 */
	private String targetId;

	public String getAttackerInstanceId() {
		return attackerInstanceId;
	}

	public void setAttackerInstanceId(String attackerInstanceId) {
		this.attackerInstanceId = attackerInstanceId;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}
}
