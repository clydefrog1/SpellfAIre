package com.spellfaire.spellfairebackend.game.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.spellfaire.spellfairebackend.game.dto.GameEvent;
import com.spellfaire.spellfairebackend.game.model.BoardCreature;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.CardZone;
import com.spellfaire.spellfairebackend.game.model.GamePlayerState;
import com.spellfaire.spellfairebackend.game.model.Keyword;
import com.spellfaire.spellfairebackend.game.model.PlayerZoneCard;
import com.spellfaire.spellfairebackend.game.model.Status;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;

/**
 * Resolves spell effects for all 24 spells in the game.
 * Hardcoded by card name for deterministic and clear behavior.
 */
@Service
public class SpellEffectResolver {

	private final CardRepository cardRepository;

	public SpellEffectResolver(CardRepository cardRepository) {
		this.cardRepository = cardRepository;
	}

	/**
	 * Resolve a spell card's effect.
	 *
	 * @param spellCard      the spell card being cast
	 * @param casterState    the state of the player casting the spell
	 * @param opponentState  the state of the opponent
	 * @param targetId       optional target identifier (creature instanceId, "ENEMY_HERO", "FRIENDLY_HERO")
	 * @return list of game events produced
	 */
	public List<GameEvent> resolveSpell(Card spellCard, GamePlayerState casterState,
										GamePlayerState opponentState, String targetId) {
		List<GameEvent> events = new ArrayList<>();
		String spellName = spellCard.getName();

		switch (spellName) {
			// === FIRE SPELLS ===
			case "Ember Bolt" -> resolveEmberBolt(casterState, opponentState, targetId, events);
			case "Searing Ping" -> resolveSearingPing(opponentState, events);
			case "Flame Javelin" -> resolveFlameJavelin(casterState, opponentState, targetId, events);
			case "Combust" -> resolveCombust(casterState, opponentState, targetId, events);
			case "Inferno Sweep" -> resolveInfernoSweep(casterState, opponentState, events);
			case "Final Spark" -> resolveFinalSpark(opponentState, events);

			// === FROST SPELLS ===
			case "Ice Shard" -> resolveIceShard(casterState, opponentState, targetId, events);
			case "Frost Shield" -> resolveFrostShield(casterState, targetId, events);
			case "Cold Snap" -> resolveColdSnap(opponentState, events);
			case "Shatter" -> resolveShatter(casterState, opponentState, targetId, events);
			case "Glacial Binding" -> resolveGlacialBinding(opponentState, targetId, events);
			case "Deep Winter" -> resolveDeepWinter(casterState, opponentState, events);

			// === NATURE SPELLS ===
			case "Mend" -> resolveMend(casterState, events);
			case "Vine Whip" -> resolveVineWhip(casterState, opponentState, targetId, events);
			case "Sproutling" -> resolveSproutlingSpell(casterState, events);
			case "Growth" -> resolveGrowth(casterState, targetId, events);
			case "Bramble Wall" -> resolveBrambleWallSpell(casterState, events);
			case "Renewal" -> resolveRenewal(casterState, events);

			// === SHADOW SPELLS ===
			case "Dark Touch" -> resolveDarkTouch(casterState, opponentState, targetId, events);
			case "Wither" -> resolveWither(opponentState, targetId, events);
			case "Siphon Life" -> resolveSiphonLife(casterState, opponentState, events);
			case "Grim Bargain" -> resolveGrimBargain(casterState, targetId, events);
			case "Haunting Fog" -> resolveHauntingFog(opponentState, events);
			case "Void Snare" -> resolveVoidSnare(casterState, opponentState, targetId, events);

			default -> events.add(GameEvent.spellResolved(spellCard.getId().toString(),
					"Unknown spell: " + spellName));
		}

		return events;
	}

	// ====== FIRE ======

	/** Deal 2 damage to any target. */
	private void resolveEmberBolt(GamePlayerState caster, GamePlayerState opponent,
								  String targetId, List<GameEvent> events) {
		dealDamageToTarget(caster, opponent, targetId, 2, "Ember Bolt", events);
	}

	/** Deal 1 damage to all enemy creatures. */
	private void resolveSearingPing(GamePlayerState opponent, List<GameEvent> events) {
		List<BoardCreature> toRemove = new ArrayList<>();
		for (BoardCreature c : new ArrayList<>(opponent.getBattlefield())) {
			int damage = applyDamageToCreature(c, 1, events, "Searing Ping");
			if (c.getHealth() <= 0) {
				toRemove.add(c);
			}
		}
		for (BoardCreature c : toRemove) {
			killCreature(opponent, c, events);
		}
	}

	/** Deal 4 damage to a creature. */
	private void resolveFlameJavelin(GamePlayerState caster, GamePlayerState opponent,
									 String targetId, List<GameEvent> events) {
		BoardCreature target = findCreature(caster, opponent, targetId);
		if (target != null) {
			applyDamageToCreature(target, 4, events, "Flame Javelin");
			if (target.getHealth() <= 0) {
				killCreatureFromEitherSide(caster, opponent, target, events);
			}
		}
	}

	/** Deal 3 damage to a creature and 2 damage to the enemy Hero. */
	private void resolveCombust(GamePlayerState caster, GamePlayerState opponent,
								String targetId, List<GameEvent> events) {
		BoardCreature target = findCreature(caster, opponent, targetId);
		if (target != null) {
			applyDamageToCreature(target, 3, events, "Combust");
			if (target.getHealth() <= 0) {
				killCreatureFromEitherSide(caster, opponent, target, events);
			}
		}
		applyDamageToHero(opponent, 2, events, "Combust");
	}

	/** Deal 2 damage to all creatures. */
	private void resolveInfernoSweep(GamePlayerState caster, GamePlayerState opponent,
									  List<GameEvent> events) {
		List<BoardCreature> toRemove = new ArrayList<>();

		for (BoardCreature c : new ArrayList<>(caster.getBattlefield())) {
			applyDamageToCreature(c, 2, events, "Inferno Sweep");
			if (c.getHealth() <= 0) toRemove.add(c);
		}
		for (BoardCreature c : new ArrayList<>(opponent.getBattlefield())) {
			applyDamageToCreature(c, 2, events, "Inferno Sweep");
			if (c.getHealth() <= 0) toRemove.add(c);
		}

		for (BoardCreature c : toRemove) {
			killCreatureFromEitherSide(caster, opponent, c, events);
		}
	}

	/** Deal 7 damage to the enemy Hero. */
	private void resolveFinalSpark(GamePlayerState opponent, List<GameEvent> events) {
		applyDamageToHero(opponent, 7, events, "Final Spark");
	}

	// ====== FROST ======

	/** Deal 1 damage to a creature. Freeze it. */
	private void resolveIceShard(GamePlayerState caster, GamePlayerState opponent,
								 String targetId, List<GameEvent> events) {
		BoardCreature target = findCreature(caster, opponent, targetId);
		if (target != null) {
			applyDamageToCreature(target, 1, events, "Ice Shard");
			if (target.getHealth() <= 0) {
				killCreatureFromEitherSide(caster, opponent, target, events);
			} else {
				freezeCreature(target, events, "Ice Shard");
			}
		}
	}

	/** Give a friendly creature +0/+3. */
	private void resolveFrostShield(GamePlayerState caster, String targetId, List<GameEvent> events) {
		BoardCreature target = findCreatureOnSide(caster, targetId);
		if (target != null) {
			target.setHealth(target.getHealth() + 3);
			target.setMaxHealth(target.getMaxHealth() + 3);
			events.add(GameEvent.buff(target.getId().toString(), 0, "Frost Shield: +0/+3"));
		}
	}

	/** Freeze all enemy creatures. */
	private void resolveColdSnap(GamePlayerState opponent, List<GameEvent> events) {
		for (BoardCreature c : opponent.getBattlefield()) {
			freezeCreature(c, events, "Cold Snap");
		}
	}

	/** Deal 5 damage to a Frozen creature. */
	private void resolveShatter(GamePlayerState caster, GamePlayerState opponent,
								String targetId, List<GameEvent> events) {
		BoardCreature target = findCreature(caster, opponent, targetId);
		if (target != null && target.getStatuses() != null && target.getStatuses().contains(Status.FROZEN)) {
			applyDamageToCreature(target, 5, events, "Shatter");
			if (target.getHealth() <= 0) {
				killCreatureFromEitherSide(caster, opponent, target, events);
			}
		}
	}

	/** Freeze an enemy creature. It takes 3 damage. */
	private void resolveGlacialBinding(GamePlayerState opponent, String targetId, List<GameEvent> events) {
		BoardCreature target = findCreatureOnSide(opponent, targetId);
		if (target != null) {
			freezeCreature(target, events, "Glacial Binding");
			applyDamageToCreature(target, 3, events, "Glacial Binding");
			if (target.getHealth() <= 0) {
				killCreature(opponent, target, events);
			}
		}
	}

	/** Draw 2 cards. Freeze a random enemy creature. */
	private void resolveDeepWinter(GamePlayerState caster, GamePlayerState opponent,
								   List<GameEvent> events) {
		drawCard(caster, events);
		drawCard(caster, events);

		if (!opponent.getBattlefield().isEmpty()) {
			int idx = (int) (Math.random() * opponent.getBattlefield().size());
			BoardCreature target = opponent.getBattlefield().get(idx);
			freezeCreature(target, events, "Deep Winter");
		}
	}

	// ====== NATURE ======

	/** Heal your Hero for 3. */
	private void resolveMend(GamePlayerState caster, List<GameEvent> events) {
		healHero(caster, 3, events, "Mend");
	}

	/** Deal 2 damage to an enemy creature. If it survives, freeze it. */
	private void resolveVineWhip(GamePlayerState caster, GamePlayerState opponent,
								 String targetId, List<GameEvent> events) {
		BoardCreature target = findCreatureOnSide(opponent, targetId);
		if (target != null) {
			applyDamageToCreature(target, 2, events, "Vine Whip");
			if (target.getHealth() <= 0) {
				killCreature(opponent, target, events);
			} else {
				freezeCreature(target, events, "Vine Whip");
			}
		}
	}

	/** Summon a 1/1 Sproutling creature. */
	private void resolveSproutlingSpell(GamePlayerState caster, List<GameEvent> events) {
		if (caster.getBattlefield().size() >= 6) return;
		Card sproutlingCard = cardRepository.findByName("Sproutling")
			.orElseThrow(() -> new IllegalStateException("Sproutling token card not found"));
		BoardCreature token = createTokenCreature(caster, sproutlingCard, 1, 1, Set.of());
		caster.getBattlefield().add(token);
		events.add(GameEvent.summon(token.getId() != null ? token.getId().toString() : "token",
				"Summoned Sproutling (1/1)"));
	}

	/** Give a friendly creature +2/+2. */
	private void resolveGrowth(GamePlayerState caster, String targetId, List<GameEvent> events) {
		BoardCreature target = findCreatureOnSide(caster, targetId);
		if (target != null) {
			target.setAttack(target.getAttack() + 2);
			target.setHealth(target.getHealth() + 2);
			target.setMaxHealth(target.getMaxHealth() + 2);
			events.add(GameEvent.buff(target.getId().toString(), 2, "Growth: +2/+2"));
		}
	}

	/** Summon a 0/6 creature with Guard. */
	private void resolveBrambleWallSpell(GamePlayerState caster, List<GameEvent> events) {
		if (caster.getBattlefield().size() >= 6) return;
		Card brambleCard = cardRepository.findByName("Bramble Wall")
			.orElseThrow(() -> new IllegalStateException("Bramble Wall token card not found"));
		BoardCreature token = createTokenCreature(caster, brambleCard, 0, 6, Set.of(Keyword.GUARD));
		caster.getBattlefield().add(token);
		events.add(GameEvent.summon(token.getId() != null ? token.getId().toString() : "token",
				"Summoned Bramble Wall (0/6 Guard)"));
	}

	/** Heal your Hero for 6. Draw a card. */
	private void resolveRenewal(GamePlayerState caster, List<GameEvent> events) {
		healHero(caster, 6, events, "Renewal");
		drawCard(caster, events);
	}

	// ====== SHADOW ======

	/** Deal 1 damage to any target. Heal your Hero for 1. */
	private void resolveDarkTouch(GamePlayerState caster, GamePlayerState opponent,
								  String targetId, List<GameEvent> events) {
		dealDamageToTarget(caster, opponent, targetId, 1, "Dark Touch", events);
		healHero(caster, 1, events, "Dark Touch");
	}

	/** Give an enemy creature -2 Attack this turn. */
	private void resolveWither(GamePlayerState opponent, String targetId, List<GameEvent> events) {
		BoardCreature target = findCreatureOnSide(opponent, targetId);
		if (target != null) {
			target.setAttack(Math.max(0, target.getAttack() - 2));
			events.add(GameEvent.buff(target.getId().toString(), -2, "Wither: -2 Attack"));
		}
	}

	/** Deal 3 damage to the enemy Hero. Heal your Hero for 3. */
	private void resolveSiphonLife(GamePlayerState caster, GamePlayerState opponent,
								   List<GameEvent> events) {
		applyDamageToHero(opponent, 3, events, "Siphon Life");
		healHero(caster, 3, events, "Siphon Life");
	}

	/** Destroy one of your creatures. Draw 2 cards. */
	private void resolveGrimBargain(GamePlayerState caster, String targetId, List<GameEvent> events) {
		BoardCreature target = findCreatureOnSide(caster, targetId);
		if (target != null) {
			killCreature(caster, target, events);
			drawCard(caster, events);
			drawCard(caster, events);
		}
	}

	/** Give all enemy creatures -1/-1. */
	private void resolveHauntingFog(GamePlayerState opponent, List<GameEvent> events) {
		List<BoardCreature> toRemove = new ArrayList<>();
		for (BoardCreature c : new ArrayList<>(opponent.getBattlefield())) {
			c.setAttack(Math.max(0, c.getAttack() - 1));
			c.setHealth(c.getHealth() - 1);
			c.setMaxHealth(Math.max(1, c.getMaxHealth() - 1));
			events.add(GameEvent.buff(c.getId().toString(), -1, "Haunting Fog: -1/-1"));
			if (c.getHealth() <= 0) {
				toRemove.add(c);
			}
		}
		for (BoardCreature c : toRemove) {
			killCreature(opponent, c, events);
		}
	}

	/** Destroy an enemy creature with cost 5 or less. */
	private void resolveVoidSnare(GamePlayerState caster, GamePlayerState opponent,
								  String targetId, List<GameEvent> events) {
		BoardCreature target = findCreatureOnSide(opponent, targetId);
		if (target != null && target.getCard().getCost() <= 5) {
			killCreature(opponent, target, events);
			events.add(GameEvent.spellResolved(target.getCard().getId().toString(),
					"Void Snare destroyed " + target.getCard().getName()));
		}
	}

	// ============ HELPER METHODS ============

	/**
	 * Deal damage to a target identifier ("ENEMY_HERO", "FRIENDLY_HERO", or creature instanceId).
	 */
	private void dealDamageToTarget(GamePlayerState caster, GamePlayerState opponent,
									String targetId, int damage, String source, List<GameEvent> events) {
		if ("ENEMY_HERO".equals(targetId)) {
			applyDamageToHero(opponent, damage, events, source);
		} else if ("FRIENDLY_HERO".equals(targetId)) {
			applyDamageToHero(caster, damage, events, source);
		} else {
			BoardCreature target = findCreature(caster, opponent, targetId);
			if (target != null) {
				applyDamageToCreature(target, damage, events, source);
				if (target.getHealth() <= 0) {
					killCreatureFromEitherSide(caster, opponent, target, events);
				}
			}
		}
	}

	/**
	 * Apply damage to a creature, respecting Ward.
	 */
	int applyDamageToCreature(BoardCreature creature, int damage, List<GameEvent> events, String source) {
		if (creature.getKeywords() != null && creature.getKeywords().contains(Keyword.WARD)) {
			creature.getKeywords().remove(Keyword.WARD);
			events.add(GameEvent.buff(creature.getId() != null ? creature.getId().toString() : "creature",
					0, source + ": Ward absorbed damage"));
			return 0;
		}
		creature.setHealth(creature.getHealth() - damage);
		events.add(GameEvent.damage(source, creature.getId() != null ? creature.getId().toString() : "creature",
				damage, source + " deals " + damage + " damage to " + creature.getCard().getName()));
		return damage;
	}

	/**
	 * Apply damage to a hero.
	 */
	void applyDamageToHero(GamePlayerState state, int damage, List<GameEvent> events, String source) {
		state.setHeroHealth(state.getHeroHealth() - damage);
		events.add(GameEvent.damage(source, state.getUserId(), damage,
				source + " deals " + damage + " damage to hero"));
	}

	/**
	 * Heal a hero (capped at 25).
	 */
	void healHero(GamePlayerState state, int amount, List<GameEvent> events, String source) {
		int before = state.getHeroHealth();
		state.setHeroHealth(Math.min(25, state.getHeroHealth() + amount));
		int healed = state.getHeroHealth() - before;
		if (healed > 0) {
			events.add(GameEvent.heal(source, state.getUserId(), healed,
					source + " heals hero for " + healed));
		}
	}

	/**
	 * Freeze a creature.
	 */
	void freezeCreature(BoardCreature creature, List<GameEvent> events, String source) {
		if (creature.getStatuses() == null) {
			creature.setStatuses(new HashSet<>());
		}
		creature.getStatuses().add(Status.FROZEN);
		events.add(GameEvent.freeze(creature.getId() != null ? creature.getId().toString() : "creature",
				source + " freezes " + creature.getCard().getName()));
	}

	/**
	 * Kill a creature and remove it from its owner's battlefield.
	 */
	void killCreature(GamePlayerState owner, BoardCreature creature, List<GameEvent> events) {
		owner.getBattlefield().remove(creature);
		events.add(GameEvent.death(creature.getId() != null ? creature.getId().toString() : "creature",
				creature.getCard().getName() + " died"));
	}

	/**
	 * Kill a creature, looking on both sides to find the owner.
	 */
	private void killCreatureFromEitherSide(GamePlayerState caster, GamePlayerState opponent,
											BoardCreature creature, List<GameEvent> events) {
		if (caster.getBattlefield().contains(creature)) {
			killCreature(caster, creature, events);
		} else {
			killCreature(opponent, creature, events);
		}
	}

	/**
	 * Draw a card for a player. Handles fatigue if deck is empty, and hand-size cap.
	 */
	void drawCard(GamePlayerState state, List<GameEvent> events) {
		List<PlayerZoneCard> deckCards = state.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.DECK)
			.sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
			.toList();

		if (deckCards.isEmpty()) {
			// Fatigue
			state.setFatigueCounter(state.getFatigueCounter() + 1);
			int fatigueDamage = state.getFatigueCounter();
			state.setHeroHealth(state.getHeroHealth() - fatigueDamage);
			events.add(GameEvent.fatigue(state.getUserId(), fatigueDamage,
					"Fatigue deals " + fatigueDamage + " damage"));
			return;
		}

		PlayerZoneCard topCard = deckCards.get(0);

		// Check hand limit
		long handSize = state.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.HAND)
			.count();

		if (handSize >= 10) {
			// Card burned
			state.getZoneCards().remove(topCard);
			events.add(GameEvent.cardDrawn(state.getUserId(),
					"Card burned (hand full): " + topCard.getCard().getName()));
			return;
		}

		// Move to hand
		topCard.setZone(CardZone.HAND);
		long maxHandPos = state.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.HAND)
			.mapToInt(PlayerZoneCard::getPosition)
			.max()
			.orElse(-1);
		topCard.setPosition((int) maxHandPos + 1);

		events.add(GameEvent.cardDrawn(state.getUserId(),
				"Drew " + topCard.getCard().getName()));
	}

	/**
	 * Find a creature by instanceId on either player's battlefield.
	 */
	BoardCreature findCreature(GamePlayerState caster, GamePlayerState opponent, String instanceId) {
		if (instanceId == null) return null;
		for (BoardCreature c : caster.getBattlefield()) {
			if (c.getId() != null && c.getId().toString().equals(instanceId)) return c;
		}
		for (BoardCreature c : opponent.getBattlefield()) {
			if (c.getId() != null && c.getId().toString().equals(instanceId)) return c;
		}
		return null;
	}

	/**
	 * Find a creature by instanceId on a specific side.
	 */
	BoardCreature findCreatureOnSide(GamePlayerState state, String instanceId) {
		if (instanceId == null) return null;
		for (BoardCreature c : state.getBattlefield()) {
			if (c.getId() != null && c.getId().toString().equals(instanceId)) return c;
		}
		return null;
	}

	/**
	 * Create a token creature (not from hand — summoned by a spell).
	 */
	private BoardCreature createTokenCreature(GamePlayerState owner, Card tokenCard,
											  int attack, int health, Set<Keyword> keywords) {
		int position = owner.getBattlefield().size();
		return new BoardCreature(owner, tokenCard, attack, health, keywords, position);
	}
}
