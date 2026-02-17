package com.spellfaire.spellfairebackend.game.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.spellfaire.spellfairebackend.game.dto.GameEvent;
import com.spellfaire.spellfairebackend.game.model.BoardCreature;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardZone;
import com.spellfaire.spellfairebackend.game.model.GamePlayerState;
import com.spellfaire.spellfairebackend.game.model.Keyword;
import com.spellfaire.spellfairebackend.game.model.PlayerZoneCard;

/**
 * Resolves creature triggers: "When played", "When this dies", and "Start of your turn".
 * Hardcoded by creature name for all creatures with special effects.
 */
@Service
public class CreatureEffectResolver {

	private final SpellEffectResolver spellHelper;

	public CreatureEffectResolver(SpellEffectResolver spellHelper) {
		this.spellHelper = spellHelper;
	}

	/**
	 * Resolve "When played" effects for a creature entering the battlefield.
	 *
	 * @param creature      the creature that was just played
	 * @param ownerState    the state of the owner
	 * @param opponentState the state of the opponent
	 * @param targetId      optional target for targeted battlecries
	 * @return list of game events
	 */
	public List<GameEvent> resolveWhenPlayed(BoardCreature creature, GamePlayerState ownerState,
											 GamePlayerState opponentState, String targetId) {
		List<GameEvent> events = new ArrayList<>();
		String name = creature.getCard().getName();

		switch (name) {
			case "Squire Captain" -> resolveSquireCaptain(creature, ownerState, targetId, events);
			case "Banner Knight" -> resolveBannerKnight(creature, ownerState, events);
			case "Chapel Healer" -> resolveChapelHealer(ownerState, events);
			case "Pack Runner" -> resolvePackRunner(creature, ownerState, events);
			case "Alpha Howler" -> resolveAlphaHowler(creature, ownerState, events);
			case "Frenzied Mauler" -> resolveFrenziedMauler(creature, ownerState, events);
			case "Bone Acolyte" -> resolveBoneAcolyte(ownerState, opponentState, targetId, events);
			case "Rotting Giant" -> resolveRottingGiant(ownerState, events);
			case "Arc Sparkbot" -> resolveArcSparkbot(opponentState, targetId, events);
			case "Plating Engineer" -> resolvePlatingEngineer(creature, ownerState, targetId, events);
			case "Overclock Colossus" -> resolveOverclockColossus(creature, ownerState, events);
			default -> { /* No "When played" effect */ }
		}

		return events;
	}

	/**
	 * Resolve "When this dies" effects.
	 */
	public List<GameEvent> resolveWhenDies(BoardCreature creature, GamePlayerState ownerState,
										   GamePlayerState opponentState) {
		List<GameEvent> events = new ArrayList<>();
		String name = creature.getCard().getName();

		switch (name) {
			case "Grave Rat" -> {
				spellHelper.drawCard(ownerState, events);
				events.add(GameEvent.cardDrawn(ownerState.getUserId(), "Grave Rat: drew a card on death"));
			}
			case "Soul Collector" -> {
				spellHelper.healHero(ownerState, 3, events, "Soul Collector");
			}
			default -> { /* No death effect */ }
		}

		return events;
	}

	/**
	 * Resolve "Start of your turn" effects for all creatures on the battlefield.
	 */
	public List<GameEvent> resolveStartOfTurn(GamePlayerState ownerState, GamePlayerState opponentState) {
		List<GameEvent> events = new ArrayList<>();

		for (BoardCreature creature : new ArrayList<>(ownerState.getBattlefield())) {
			String name = creature.getCard().getName();

			switch (name) {
				case "Royal Tactician" -> resolveRoyalTactician(ownerState, events);
				case "Lich Adept" -> resolveLichAdept(ownerState, events);
				default -> { /* No start-of-turn effect */ }
			}
		}

		return events;
	}

	// ====== WHEN PLAYED IMPLEMENTATIONS ======

	/** When played, give another friendly creature +1 Health. */
	private void resolveSquireCaptain(BoardCreature self, GamePlayerState owner,
									  String targetId, List<GameEvent> events) {
		BoardCreature target = findFriendlyTarget(owner, self, targetId);
		if (target != null) {
			target.setHealth(target.getHealth() + 1);
			target.setMaxHealth(target.getMaxHealth() + 1);
			events.add(GameEvent.buff(target.getId().toString(), 0,
					"Squire Captain: +1 Health to " + target.getCard().getName()));
		}
	}

	/** When played, if you control a Guard creature, gain +1 Attack. */
	private void resolveBannerKnight(BoardCreature self, GamePlayerState owner, List<GameEvent> events) {
		boolean hasGuard = owner.getBattlefield().stream()
			.filter(c -> c != self)
			.anyMatch(c -> c.getKeywords() != null && c.getKeywords().contains(Keyword.GUARD));
		if (hasGuard) {
			self.setAttack(self.getAttack() + 1);
			events.add(GameEvent.buff(self.getId() != null ? self.getId().toString() : "self",
					1, "Banner Knight: +1 Attack (Guard present)"));
		}
	}

	/** When played, heal your Hero for 3. */
	private void resolveChapelHealer(GamePlayerState owner, List<GameEvent> events) {
		spellHelper.healHero(owner, 3, events, "Chapel Healer");
	}

	/** When played, if you control another creature, gain +1 Attack. */
	private void resolvePackRunner(BoardCreature self, GamePlayerState owner, List<GameEvent> events) {
		boolean hasOther = owner.getBattlefield().stream().anyMatch(c -> c != self);
		if (hasOther) {
			self.setAttack(self.getAttack() + 1);
			events.add(GameEvent.buff(self.getId() != null ? self.getId().toString() : "self",
					1, "Pack Runner: +1 Attack (another creature present)"));
		}
	}

	/** When played, give your other creatures +1 Attack this turn. */
	private void resolveAlphaHowler(BoardCreature self, GamePlayerState owner, List<GameEvent> events) {
		for (BoardCreature c : owner.getBattlefield()) {
			if (c != self) {
				c.setAttack(c.getAttack() + 1);
				events.add(GameEvent.buff(c.getId().toString(), 1,
						"Alpha Howler: +1 Attack to " + c.getCard().getName()));
			}
		}
	}

	/** When played, deal 1 damage to your Hero. Gain Charge. */
	private void resolveFrenziedMauler(BoardCreature self, GamePlayerState owner, List<GameEvent> events) {
		spellHelper.applyDamageToHero(owner, 1, events, "Frenzied Mauler");
		self.setCanAttack(true);
		if (self.getKeywords() == null) {
			self.setKeywords(new HashSet<>());
		}
		self.getKeywords().add(Keyword.CHARGE);
		events.add(GameEvent.buff(self.getId() != null ? self.getId().toString() : "self",
				0, "Frenzied Mauler gains Charge"));
	}

	/** When played, deal 1 damage to any target. */
	private void resolveBoneAcolyte(GamePlayerState owner, GamePlayerState opponent,
									String targetId, List<GameEvent> events) {
		if (targetId != null) {
			if ("ENEMY_HERO".equals(targetId)) {
				spellHelper.applyDamageToHero(opponent, 1, events, "Bone Acolyte");
			} else if ("FRIENDLY_HERO".equals(targetId)) {
				spellHelper.applyDamageToHero(owner, 1, events, "Bone Acolyte");
			} else {
				BoardCreature target = spellHelper.findCreature(owner, opponent, targetId);
				if (target != null) {
					spellHelper.applyDamageToCreature(target, 1, events, "Bone Acolyte");
					if (target.getHealth() <= 0) {
						if (owner.getBattlefield().contains(target)) {
							spellHelper.killCreature(owner, target, events);
						} else {
							spellHelper.killCreature(opponent, target, events);
						}
					}
				}
			}
		}
	}

	/** When played, you take 2 damage. */
	private void resolveRottingGiant(GamePlayerState owner, List<GameEvent> events) {
		spellHelper.applyDamageToHero(owner, 2, events, "Rotting Giant");
	}

	/** When played, deal 1 damage to an enemy creature. */
	private void resolveArcSparkbot(GamePlayerState opponent, String targetId, List<GameEvent> events) {
		BoardCreature target = spellHelper.findCreatureOnSide(opponent, targetId);
		if (target != null) {
			spellHelper.applyDamageToCreature(target, 1, events, "Arc Sparkbot");
			if (target.getHealth() <= 0) {
				spellHelper.killCreature(opponent, target, events);
			}
		}
	}

	/** When played, give another friendly creature Ward. */
	private void resolvePlatingEngineer(BoardCreature self, GamePlayerState owner,
										String targetId, List<GameEvent> events) {
		BoardCreature target = findFriendlyTarget(owner, self, targetId);
		if (target != null) {
			if (target.getKeywords() == null) {
				target.setKeywords(new HashSet<>());
			}
			target.getKeywords().add(Keyword.WARD);
			events.add(GameEvent.buff(target.getId().toString(), 0,
					"Plating Engineer: Ward to " + target.getCard().getName()));
		}
	}

	/** When played, deal 2 damage to your Hero. Gain +1 Attack. */
	private void resolveOverclockColossus(BoardCreature self, GamePlayerState owner, List<GameEvent> events) {
		spellHelper.applyDamageToHero(owner, 2, events, "Overclock Colossus");
		self.setAttack(self.getAttack() + 1);
		events.add(GameEvent.buff(self.getId() != null ? self.getId().toString() : "self",
				1, "Overclock Colossus: +1 Attack"));
	}

	// ====== START OF TURN IMPLEMENTATIONS ======

	/** Start of your turn: give a random friendly creature +1/+1. */
	private void resolveRoyalTactician(GamePlayerState owner, List<GameEvent> events) {
		if (owner.getBattlefield().isEmpty()) return;
		int idx = (int) (Math.random() * owner.getBattlefield().size());
		BoardCreature target = owner.getBattlefield().get(idx);
		target.setAttack(target.getAttack() + 1);
		target.setHealth(target.getHealth() + 1);
		target.setMaxHealth(target.getMaxHealth() + 1);
		events.add(GameEvent.buff(target.getId().toString(), 1,
				"Royal Tactician: +1/+1 to " + target.getCard().getName()));
	}

	/** Start of your turn: return a random creature that died (cost ≤ 3) to your hand. */
	private void resolveLichAdept(GamePlayerState owner, List<GameEvent> events) {
		List<PlayerZoneCard> discarded = owner.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.DISCARD)
			.filter(c -> c.getCard().getCost() <= 3)
			.filter(c -> c.getCard().getAttack() != null) // Must be a creature
			.toList();

		if (discarded.isEmpty()) return;

		int idx = (int) (Math.random() * discarded.size());
		PlayerZoneCard revived = discarded.get(idx);
		revived.setZone(CardZone.HAND);

		long maxHandPos = owner.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.HAND)
			.mapToInt(PlayerZoneCard::getPosition)
			.max()
			.orElse(-1);
		revived.setPosition((int) maxHandPos + 1);

		events.add(GameEvent.cardDrawn(owner.getUserId(),
				"Lich Adept returned " + revived.getCard().getName() + " to hand"));
	}

	// ====== HELPERS ======

	/**
	 * Find a friendly creature (other than self) by instanceId, or pick a random one if no target given.
	 */
	private BoardCreature findFriendlyTarget(GamePlayerState owner, BoardCreature self, String targetId) {
		if (targetId != null) {
			return owner.getBattlefield().stream()
				.filter(c -> c != self && c.getId() != null && c.getId().toString().equals(targetId))
				.findFirst()
				.orElse(null);
		}
		// Auto-pick: first other friendly creature
		return owner.getBattlefield().stream()
			.filter(c -> c != self)
			.findFirst()
			.orElse(null);
	}
}
