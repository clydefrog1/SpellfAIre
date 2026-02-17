package com.spellfaire.spellfairebackend.game.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.spellfaire.spellfairebackend.game.dto.AttackRequest;
import com.spellfaire.spellfairebackend.game.dto.GameActionResponse;
import com.spellfaire.spellfairebackend.game.dto.GameEvent;
import com.spellfaire.spellfairebackend.game.dto.PlayCardRequest;
import com.spellfaire.spellfairebackend.game.model.BoardCreature;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.CardZone;
import com.spellfaire.spellfairebackend.game.model.Game;
import com.spellfaire.spellfairebackend.game.model.GamePlayerState;
import com.spellfaire.spellfairebackend.game.model.Keyword;
import com.spellfaire.spellfairebackend.game.model.PlayerZoneCard;
import com.spellfaire.spellfairebackend.game.model.Status;

/**
 * Heuristic-based AI opponent.
 * Executes a full turn: plays cards greedily, then attacks optimally.
 */
@Service
public class AiService {

	private static final Logger log = LoggerFactory.getLogger(AiService.class);

	private final GameplayService gameplayService;

	public AiService(GameplayService gameplayService) {
		this.gameplayService = gameplayService;
	}

	/**
	 * Execute a full AI turn.
	 * Returns all game events that occurred during the AI's actions.
	 */
	public List<GameEvent> executeTurn(Game game) {
		List<GameEvent> allEvents = new ArrayList<>();

		GamePlayerState aiState = gameplayService.getPlayerState(game, "AI");
		GamePlayerState humanState = gameplayService.getOpponentState(game, "AI");

		// Phase 1: Check for lethal
		if (canDoLethal(aiState, humanState)) {
			allEvents.addAll(executeLethal(game, aiState, humanState));
			return allEvents;
		}

		// Phase 2: Play cards (highest cost first)
		allEvents.addAll(playCardsPhase(game, aiState));

		// Phase 3: Attack
		allEvents.addAll(attackPhase(game, aiState, humanState));

		return allEvents;
	}

	/**
	 * Check if AI can deliver lethal this turn.
	 */
	private boolean canDoLethal(GamePlayerState aiState, GamePlayerState humanState) {
		int totalDamage = 0;

		// Calculate available creature attack damage
		boolean enemyHasGuard = humanState.getBattlefield().stream()
			.anyMatch(c -> c.getKeywords() != null && c.getKeywords().contains(Keyword.GUARD));

		if (!enemyHasGuard) {
			for (BoardCreature c : aiState.getBattlefield()) {
				if (c.isCanAttack() && !c.isHasAttackedThisTurn()
						&& (c.getStatuses() == null || !c.getStatuses().contains(Status.FROZEN))) {
					totalDamage += c.getAttack();
				}
			}
		} else {
			// Need to kill guards first, then remaining creatures go face
			// Simplified: just count non-guard-blocked damage as 0 for now
		}

		// Count burn spell damage from hand
		for (PlayerZoneCard zc : aiState.getZoneCards()) {
			if (zc.getZone() != CardZone.HAND) continue;
			Card card = zc.getCard();
			if (card.getCardType() != CardType.SPELL) continue;
			if (card.getCost() > aiState.getCurrentMana()) continue;

			int burnDmg = getBurnDamage(card);
			if (burnDmg > 0) {
				totalDamage += burnDmg;
			}
		}

		return totalDamage >= humanState.getHeroHealth();
	}

	/**
	 * Execute lethal sequence: burn spells first, then all-face attacks.
	 */
	private List<GameEvent> executeLethal(Game game, GamePlayerState aiState, GamePlayerState humanState) {
		List<GameEvent> events = new ArrayList<>();

		// Play burn spells first
		List<PlayerZoneCard> handCards = new ArrayList<>(aiState.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.HAND)
			.filter(c -> c.getCard().getCardType() == CardType.SPELL)
			.sorted(Comparator.comparingInt((PlayerZoneCard c) -> c.getCard().getCost()).reversed())
			.toList());

		for (PlayerZoneCard zc : handCards) {
			if (humanState.getHeroHealth() <= 0) break;
			Card card = zc.getCard();
			int burn = getBurnDamage(card);
			if (burn > 0 && card.getCost() <= aiState.getCurrentMana()) {
				PlayCardRequest req = new PlayCardRequest();
				req.setCardId(card.getId().toString());
				req.setTargetId("ENEMY_HERO");
				GameActionResponse resp = gameplayService.playCardInternal(game, "AI", req);
				if (resp != null) events.addAll(resp.getEvents());
			}
		}

		// Then attack face with everything
		for (BoardCreature creature : new ArrayList<>(aiState.getBattlefield())) {
			if (humanState.getHeroHealth() <= 0) break;
			if (!creature.isCanAttack() || creature.isHasAttackedThisTurn()) continue;
			if (creature.getStatuses() != null && creature.getStatuses().contains(Status.FROZEN)) continue;

			AttackRequest req = new AttackRequest();
			req.setAttackerInstanceId(creature.getId().toString());
			req.setTargetId("ENEMY_HERO");
			GameActionResponse resp = gameplayService.attackInternal(game, "AI", req);
			if (resp != null) events.addAll(resp.getEvents());
		}

		return events;
	}

	/**
	 * Play cards from hand, prioritizing highest-cost cards first.
	 */
	private List<GameEvent> playCardsPhase(Game game, GamePlayerState aiState) {
		List<GameEvent> events = new ArrayList<>();

		boolean playedSomething = true;
		while (playedSomething) {
			playedSomething = false;

			// Get current hand cards sorted by cost descending
			List<PlayerZoneCard> hand = aiState.getZoneCards().stream()
				.filter(c -> c.getZone() == CardZone.HAND)
				.sorted(Comparator.comparingInt((PlayerZoneCard c) -> c.getCard().getCost()).reversed())
				.toList();

			for (PlayerZoneCard zc : hand) {
				Card card = zc.getCard();
				if (card.getCost() > aiState.getCurrentMana()) continue;

				if (card.getCardType() == CardType.CREATURE && aiState.getBattlefield().size() >= 6) {
					continue;
				}

				PlayCardRequest req = new PlayCardRequest();
				req.setCardId(card.getId().toString());
				// Target is auto-picked by GameplayService.pickAiSpellTarget for spells

				GameActionResponse resp = gameplayService.playCardInternal(game, "AI", req);
				if (resp != null) {
					events.addAll(resp.getEvents());
					playedSomething = true;
					break; // Re-evaluate hand after each play
				}
			}
		}

		return events;
	}

	/**
	 * Attack phase: value trades first, then go face.
	 */
	private List<GameEvent> attackPhase(Game game, GamePlayerState aiState, GamePlayerState humanState) {
		List<GameEvent> events = new ArrayList<>();

		boolean enemyHasGuard = humanState.getBattlefield().stream()
			.anyMatch(c -> c.getKeywords() != null && c.getKeywords().contains(Keyword.GUARD));

		for (BoardCreature attacker : new ArrayList<>(aiState.getBattlefield())) {
			if (!attacker.isCanAttack() || attacker.isHasAttackedThisTurn()) continue;
			if (attacker.getStatuses() != null && attacker.getStatuses().contains(Status.FROZEN)) continue;

			// Recalculate guard status each iteration (a guard might have died)
			boolean guardPresent = humanState.getBattlefield().stream()
				.anyMatch(c -> c.getKeywords() != null && c.getKeywords().contains(Keyword.GUARD));

			String targetId = pickAttackTarget(attacker, aiState, humanState, guardPresent);
			if (targetId == null) continue;

			AttackRequest req = new AttackRequest();
			req.setAttackerInstanceId(attacker.getId().toString());
			req.setTargetId(targetId);

			GameActionResponse resp = gameplayService.attackInternal(game, "AI", req);
			if (resp != null) {
				events.addAll(resp.getEvents());
			}
		}

		return events;
	}

	/**
	 * Pick the best attack target for a creature.
	 */
	private String pickAttackTarget(BoardCreature attacker, GamePlayerState aiState,
									GamePlayerState humanState, boolean guardPresent) {
		List<BoardCreature> enemies = humanState.getBattlefield();

		if (guardPresent) {
			// Must attack guard creatures
			BoardCreature guard = enemies.stream()
				.filter(c -> c.getKeywords() != null && c.getKeywords().contains(Keyword.GUARD))
				.min(Comparator.comparingInt(BoardCreature::getHealth))
				.orElse(null);
			return guard != null ? guard.getId().toString() : null;
		}

		// Look for value trades: can kill a creature without dying
		BoardCreature valueTrade = enemies.stream()
			.filter(c -> c.getHealth() <= attacker.getAttack()) // Attacker can kill it
			.filter(c -> c.getAttack() < attacker.getHealth())  // Attacker survives
			.max(Comparator.comparingInt(c -> c.getAttack() + c.getHealth())) // Kill highest value
			.orElse(null);

		if (valueTrade != null) {
			return valueTrade.getId().toString();
		}

		// Look for trades where both die but we trade up (kill higher cost)
		BoardCreature tradeUp = enemies.stream()
			.filter(c -> c.getHealth() <= attacker.getAttack())
			.filter(c -> c.getCard().getCost() > attacker.getCard().getCost())
			.max(Comparator.comparingInt(c -> c.getCard().getCost()))
			.orElse(null);

		if (tradeUp != null) {
			return tradeUp.getId().toString();
		}

		// Go face
		return "ENEMY_HERO";
	}

	/**
	 * Get burn damage to hero for a given spell (0 if not a burn spell).
	 */
	private int getBurnDamage(Card spell) {
		return switch (spell.getName()) {
			case "Ember Bolt" -> 2;
			case "Final Spark" -> 7;
			case "Dark Touch" -> 1;
			case "Siphon Life" -> 3;
			case "Combust" -> 2; // Only the hero portion
			default -> 0;
		};
	}
}
