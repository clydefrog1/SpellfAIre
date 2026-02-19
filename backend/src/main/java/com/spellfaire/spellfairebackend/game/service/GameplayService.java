package com.spellfaire.spellfairebackend.game.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.UserRepository;
import com.spellfaire.spellfairebackend.game.dto.AttackRequest;
import com.spellfaire.spellfairebackend.game.dto.CreateAiGameRequest;
import com.spellfaire.spellfairebackend.game.dto.GameActionResponse;
import com.spellfaire.spellfairebackend.game.dto.GameEvent;
import com.spellfaire.spellfairebackend.game.dto.PlayCardRequest;
import com.spellfaire.spellfairebackend.game.model.BoardCreature;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.CardZone;
import com.spellfaire.spellfairebackend.game.model.Deck;
import com.spellfaire.spellfairebackend.game.model.DeckCard;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.Game;
import com.spellfaire.spellfairebackend.game.model.GamePhase;
import com.spellfaire.spellfairebackend.game.model.GamePlayerState;
import com.spellfaire.spellfairebackend.game.model.GameStatus;
import com.spellfaire.spellfairebackend.game.model.Keyword;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;
import com.spellfaire.spellfairebackend.game.model.PlayerZoneCard;
import com.spellfaire.spellfairebackend.game.model.Status;
import com.spellfaire.spellfairebackend.game.repo.GameRepository;

/**
 * Core gameplay engine. Handles all game actions: playing cards, attacking,
 * ending turns, surrendering, and AI game creation with full turn flow.
 */
@Service
public class GameplayService {

	private static final Logger log = LoggerFactory.getLogger(GameplayService.class);

	private final GameRepository gameRepository;
	private final GameService gameService;
	private final DeckService deckService;
	private final UserRepository userRepository;
	private final SpellEffectResolver spellResolver;
	private final CreatureEffectResolver creatureResolver;
	private final AiService aiService;
	private final Random random;

	public GameplayService(GameRepository gameRepository, GameService gameService,
						   DeckService deckService, UserRepository userRepository,
						   SpellEffectResolver spellResolver, CreatureEffectResolver creatureResolver,
						   @Lazy AiService aiService) {
		this.gameRepository = gameRepository;
		this.gameService = gameService;
		this.deckService = deckService;
		this.userRepository = userRepository;
		this.spellResolver = spellResolver;
		this.creatureResolver = creatureResolver;
		this.aiService = aiService;
		this.random = new Random();
	}

	// ==================================================================
	// CREATE AI GAME
	// ==================================================================

	/**
	 * Create a new game against the AI.
	 * Auto-generates decks for both player and AI, initializes states,
	 * starts the game, and if AI goes first, executes the AI turn.
	 */
	@Transactional
	public GameActionResponse createAiGame(String playerId, CreateAiGameRequest request) {
		User player = userRepository.findById(UUID.fromString(playerId))
			.orElseThrow(() -> new IllegalArgumentException("User not found"));

		// Build player deck
		Deck playerDeck = deckService.buildAutoDeck(player, request.getFaction(), request.getMagicSchool());

		// Build AI deck (different faction/school for variety)
		Faction aiFaction = pickDifferentFaction(request.getFaction());
		MagicSchool aiSchool = pickDifferentSchool(request.getMagicSchool());
		Deck aiDeck = deckService.buildAutoDeck(player, aiFaction, aiSchool); // Uses same user for FK

		// Create game
		Game game = new Game();
		game.setPlayer1Id(playerId);
		game.setPlayer2Id("AI");

		boolean playerFirst = random.nextBoolean();
		game.setCurrentPlayerId(playerFirst ? playerId : "AI");

		// Initialize player states
		initializePlayerState(game.getPlayer1State(), playerId, playerDeck, playerFirst);
		initializePlayerState(game.getPlayer2State(), "AI", aiDeck, !playerFirst);

		game.setGameStatus(GameStatus.IN_PROGRESS);
		game.setCurrentPhase(GamePhase.MAIN);
		game.setTurnNumber(1);

		Instant now = Instant.now();
		game.setCreatedAt(now);
		game.setUpdatedAt(now);

		game = gameRepository.save(game);

		List<GameEvent> events = new ArrayList<>();

		// Start-of-turn for first player
		GamePlayerState firstPlayerState = playerFirst ? game.getPlayer1State() : game.getPlayer2State();
		events.addAll(processStartOfTurn(game, firstPlayerState));

		// If AI goes first, execute AI turn
		if (!playerFirst) {
			events.addAll(executeAiTurn(game));
			events.addAll(checkGameOver(game));

			if (game.getGameStatus() == GameStatus.IN_PROGRESS && "AI".equals(game.getCurrentPlayerId())) {
				game.setCurrentPlayerId(game.getPlayer1Id());
				game.setTurnNumber(game.getTurnNumber() + 1);

				GamePlayerState playerState = getPlayerState(game, game.getPlayer1Id());
				events.addAll(processStartOfTurn(game, playerState));
				events.addAll(checkGameOver(game));
			}
		}

		events.addAll(checkGameOver(game));

		game.setUpdatedAt(Instant.now());
		game = gameRepository.save(game);

		return new GameActionResponse(gameService.toGameResponse(game), events);
	}

	// ==================================================================
	// PLAY CARD
	// ==================================================================

	@Transactional
	public GameActionResponse playCard(UUID gameId, String playerId, PlayCardRequest request) {
		Game game = loadGame(gameId);
		validatePlayerTurn(game, playerId);
		validatePhase(game, GamePhase.MAIN);

		GamePlayerState playerState = getPlayerState(game, playerId);
		GamePlayerState opponentState = getOpponentState(game, playerId);

		// Find the card in hand
		PlayerZoneCard handCard = playerState.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.HAND)
			.filter(c -> c.getCard().getId().toString().equals(request.getCardId()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Card not in hand"));

		Card card = handCard.getCard();

		// Check mana
		if (card.getCost() > playerState.getCurrentMana()) {
			throw new IllegalArgumentException("Not enough mana");
		}

		List<GameEvent> events = new ArrayList<>();

		// Deduct mana
		playerState.setCurrentMana(playerState.getCurrentMana() - card.getCost());

		if (card.getCardType() == CardType.CREATURE) {
			// Check board space
			if (playerState.getBattlefield().size() >= 6) {
				throw new IllegalArgumentException("Battlefield is full");
			}

			// Remove from hand
			playerState.getZoneCards().remove(handCard);

			// Create BoardCreature
			boolean hasCharge = card.getKeywords() != null && card.getKeywords().contains(Keyword.CHARGE);
			int position = playerState.getBattlefield().size();
			BoardCreature creature = new BoardCreature(
				playerState, card,
				card.getAttack() != null ? card.getAttack() : 0,
				card.getHealth() != null ? card.getHealth() : 1,
				card.getKeywords() != null ? new HashSet<>(card.getKeywords()) : new HashSet<>(),
				position
			);
			creature.setCanAttack(hasCharge);
			creature.setHasAttackedThisTurn(false);
			playerState.getBattlefield().add(creature);

			events.add(GameEvent.cardPlayed(card.getId().toString(),
					"Played " + card.getName()));

			// Resolve "When played" effects
			// Save first so the creature gets an ID
			game.setUpdatedAt(Instant.now());
			game = gameRepository.save(game);

			events.addAll(creatureResolver.resolveWhenPlayed(creature, playerState, opponentState,
					request.getTargetId()));

		} else if (card.getCardType() == CardType.SPELL) {
			// Remove from hand, move to discard
			handCard.setZone(CardZone.DISCARD);
			handCard.setPosition(0);

			events.add(GameEvent.cardPlayed(card.getId().toString(),
					"Cast " + card.getName()));

			// Resolve spell effect
			events.addAll(spellResolver.resolveSpell(card, playerState, opponentState,
					request.getTargetId()));
		}

		// Check for game over
		events.addAll(checkGameOver(game));

		game.setUpdatedAt(Instant.now());
		game = gameRepository.save(game);

		return new GameActionResponse(gameService.toGameResponse(game), events);
	}

	// ==================================================================
	// ATTACK
	// ==================================================================

	@Transactional
	public GameActionResponse attack(UUID gameId, String playerId, AttackRequest request) {
		Game game = loadGame(gameId);
		validatePlayerTurn(game, playerId);
		validatePhase(game, GamePhase.MAIN);

		GamePlayerState attackerState = getPlayerState(game, playerId);
		GamePlayerState defenderState = getOpponentState(game, playerId);

		// Find attacker
		BoardCreature attacker = attackerState.getBattlefield().stream()
			.filter(c -> c.getId().toString().equals(request.getAttackerInstanceId()))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Attacker not found"));

		// Validate attacker can attack
		if (!attacker.isCanAttack()) {
			throw new IllegalArgumentException("Creature cannot attack (summoning sickness)");
		}
		if (attacker.isHasAttackedThisTurn()) {
			throw new IllegalArgumentException("Creature already attacked this turn");
		}
		if (attacker.getStatuses() != null && attacker.getStatuses().contains(Status.FROZEN)) {
			throw new IllegalArgumentException("Creature is frozen");
		}

		List<GameEvent> events = new ArrayList<>();

		// Enforce Guard
		boolean enemyHasGuard = defenderState.getBattlefield().stream()
			.anyMatch(c -> c.getKeywords() != null && c.getKeywords().contains(Keyword.GUARD));

		String targetId = request.getTargetId();

		if ("ENEMY_HERO".equals(targetId)) {
			if (enemyHasGuard) {
				throw new IllegalArgumentException("Must attack a Guard creature first");
			}

			// Attack hero
			int damage = attacker.getAttack();
			defenderState.setHeroHealth(defenderState.getHeroHealth() - damage);
			attacker.setHasAttackedThisTurn(true);

			events.add(GameEvent.attack(attacker.getId().toString(), "ENEMY_HERO",
					attacker.getCard().getName() + " attacks enemy hero for " + damage));
			events.add(GameEvent.damage(attacker.getId().toString(), defenderState.getUserId(),
					damage, attacker.getCard().getName() + " deals " + damage + " to hero"));

			// Lifesteal
			if (attacker.getKeywords() != null && attacker.getKeywords().contains(Keyword.LIFESTEAL)) {
				spellResolver.healHero(attackerState, damage, events, attacker.getCard().getName() + " (Lifesteal)");
			}
		} else {
			// Attack creature
			BoardCreature defender = defenderState.getBattlefield().stream()
				.filter(c -> c.getId().toString().equals(targetId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Target creature not found"));

			if (enemyHasGuard && (defender.getKeywords() == null || !defender.getKeywords().contains(Keyword.GUARD))) {
				throw new IllegalArgumentException("Must attack a Guard creature first");
			}

			events.add(GameEvent.attack(attacker.getId().toString(), defender.getId().toString(),
					attacker.getCard().getName() + " attacks " + defender.getCard().getName()));

			// Simultaneous damage
			int attackerDamage = attacker.getAttack();
			int defenderDamage = defender.getAttack();

			// Apply damage to defender (respecting Ward)
			int actualDamageToDefender = spellResolver.applyDamageToCreature(defender, attackerDamage, events,
					attacker.getCard().getName());
			// Apply damage to attacker (respecting Ward)
			int actualDamageToAttacker = spellResolver.applyDamageToCreature(attacker, defenderDamage, events,
					defender.getCard().getName());

			attacker.setHasAttackedThisTurn(true);

			// Lifesteal for attacker
			if (attacker.getKeywords() != null && attacker.getKeywords().contains(Keyword.LIFESTEAL)
					&& actualDamageToDefender > 0) {
				spellResolver.healHero(attackerState, actualDamageToDefender, events,
						attacker.getCard().getName() + " (Lifesteal)");
			}
			// Lifesteal for defender
			if (defender.getKeywords() != null && defender.getKeywords().contains(Keyword.LIFESTEAL)
					&& actualDamageToAttacker > 0) {
				spellResolver.healHero(defenderState, actualDamageToAttacker, events,
						defender.getCard().getName() + " (Lifesteal)");
			}

			// Check deaths
			List<BoardCreature> dead = new ArrayList<>();
			if (defender.getHealth() <= 0) dead.add(defender);
			if (attacker.getHealth() <= 0) dead.add(attacker);

			for (BoardCreature deadCreature : dead) {
				GamePlayerState owner = attackerState.getBattlefield().contains(deadCreature)
						? attackerState : defenderState;

				// Move card to discard pile for potential resurrection
				moveCreatureToDiscard(owner, deadCreature);

				spellResolver.killCreature(owner, deadCreature, events);

				// Death triggers
				events.addAll(creatureResolver.resolveWhenDies(deadCreature, owner,
						owner == attackerState ? defenderState : attackerState));
			}
		}

		// Check game over
		events.addAll(checkGameOver(game));

		game.setUpdatedAt(Instant.now());
		game = gameRepository.save(game);

		return new GameActionResponse(gameService.toGameResponse(game), events);
	}

	// ==================================================================
	// END TURN
	// ==================================================================

	@Transactional
	public GameActionResponse endTurn(UUID gameId, String playerId) {
		Game game = loadGame(gameId);
		validatePlayerTurn(game, playerId);

		List<GameEvent> events = new ArrayList<>();

		// End-of-turn: nothing specific in current rules, but placeholder for future

		// Switch turns
		String nextPlayerId = game.getCurrentPlayerId().equals(game.getPlayer1Id())
				? game.getPlayer2Id() : game.getPlayer1Id();
		game.setCurrentPlayerId(nextPlayerId);
		game.setTurnNumber(game.getTurnNumber() + 1);

		// Start of next player's turn
		GamePlayerState nextPlayerState = getPlayerState(game, nextPlayerId);
		events.addAll(processStartOfTurn(game, nextPlayerState));

		// If AI's turn, execute full AI turn then switch back to player
		if ("AI".equals(nextPlayerId)) {
			events.addAll(executeAiTurn(game));
			events.addAll(checkGameOver(game));

			if (game.getGameStatus() == GameStatus.IN_PROGRESS && "AI".equals(game.getCurrentPlayerId())) {
				// AI turn is complete, switch back to player
				game.setCurrentPlayerId(game.getPlayer1Id());
				game.setTurnNumber(game.getTurnNumber() + 1);

				// Start player's turn
				GamePlayerState playerState = getPlayerState(game, game.getPlayer1Id());
				events.addAll(processStartOfTurn(game, playerState));
				events.addAll(checkGameOver(game));
			}
		}

		// Check game over
		events.addAll(checkGameOver(game));

		game.setUpdatedAt(Instant.now());
		game = gameRepository.save(game);

		return new GameActionResponse(gameService.toGameResponse(game), events);
	}

	// ==================================================================
	// SURRENDER
	// ==================================================================

	@Transactional
	public GameActionResponse surrender(UUID gameId, String playerId) {
		Game game = loadGame(gameId);

		if (game.getGameStatus() == GameStatus.FINISHED) {
			throw new IllegalArgumentException("Game is already finished");
		}

		String winnerId = playerId.equals(game.getPlayer1Id())
				? game.getPlayer2Id() : game.getPlayer1Id();

		game.setGameStatus(GameStatus.FINISHED);
		game.setWinnerId(winnerId);
		game.setUpdatedAt(Instant.now());
		game = gameRepository.save(game);

		List<GameEvent> events = new ArrayList<>();
		events.add(GameEvent.gameOver(winnerId, playerId + " surrendered"));

		return new GameActionResponse(gameService.toGameResponse(game), events);
	}

	// ==================================================================
	// AI TURN EXECUTION (delegated to AiService when available, simple fallback here)
	// ==================================================================

	/**
	 * Execute a full AI turn, delegated to AiService.
	 */
	List<GameEvent> executeAiTurn(Game game) {
		return aiService.executeTurn(game);
	}

	/**
	 * Allow AiService to call internal methods for AI turn execution.
	 */
	GameActionResponse playCardInternal(Game game, String playerId, PlayCardRequest request) {
		GamePlayerState playerState = getPlayerState(game, playerId);
		GamePlayerState opponentState = getOpponentState(game, playerId);

		PlayerZoneCard handCard = playerState.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.HAND)
			.filter(c -> c.getCard().getId().toString().equals(request.getCardId()))
			.findFirst()
			.orElse(null);

		if (handCard == null) return null;

		Card card = handCard.getCard();
		if (card.getCost() > playerState.getCurrentMana()) return null;

		List<GameEvent> events = new ArrayList<>();
		playerState.setCurrentMana(playerState.getCurrentMana() - card.getCost());

		if (card.getCardType() == CardType.CREATURE) {
			if (playerState.getBattlefield().size() >= 6) return null;

			playerState.getZoneCards().remove(handCard);

			boolean hasCharge = card.getKeywords() != null && card.getKeywords().contains(Keyword.CHARGE);
			int position = playerState.getBattlefield().size();
			BoardCreature creature = new BoardCreature(
				playerState, card,
				card.getAttack() != null ? card.getAttack() : 0,
				card.getHealth() != null ? card.getHealth() : 1,
				card.getKeywords() != null ? new HashSet<>(card.getKeywords()) : new HashSet<>(),
				position
			);
			creature.setCanAttack(hasCharge);
			creature.setHasAttackedThisTurn(false);
			playerState.getBattlefield().add(creature);

			events.add(GameEvent.cardPlayed(card.getId().toString(), "AI played " + card.getName()));

			// Need to flush to get creature ID
			gameRepository.save(game);

			events.addAll(creatureResolver.resolveWhenPlayed(creature, playerState, opponentState, null));

		} else if (card.getCardType() == CardType.SPELL) {
			handCard.setZone(CardZone.DISCARD);
			handCard.setPosition(0);
			events.add(GameEvent.cardPlayed(card.getId().toString(), "AI cast " + card.getName()));

			// AI picks targets for spells
			String targetId = pickAiSpellTarget(card, playerState, opponentState);
			events.addAll(spellResolver.resolveSpell(card, playerState, opponentState, targetId));
		}

		return new GameActionResponse(null, events);
	}

	GameActionResponse attackInternal(Game game, String playerId, AttackRequest request) {
		GamePlayerState attackerState = getPlayerState(game, playerId);
		GamePlayerState defenderState = getOpponentState(game, playerId);

		BoardCreature attacker = attackerState.getBattlefield().stream()
			.filter(c -> c.getId() != null && c.getId().toString().equals(request.getAttackerInstanceId()))
			.findFirst()
			.orElse(null);

		if (attacker == null || !attacker.isCanAttack() || attacker.isHasAttackedThisTurn()) return null;
		if (attacker.getStatuses() != null && attacker.getStatuses().contains(Status.FROZEN)) return null;

		List<GameEvent> events = new ArrayList<>();

		boolean enemyHasGuard = defenderState.getBattlefield().stream()
			.anyMatch(c -> c.getKeywords() != null && c.getKeywords().contains(Keyword.GUARD));

		String targetId = request.getTargetId();

		if ("ENEMY_HERO".equals(targetId)) {
			if (enemyHasGuard) return null;

			int damage = attacker.getAttack();
			defenderState.setHeroHealth(defenderState.getHeroHealth() - damage);
			attacker.setHasAttackedThisTurn(true);

			events.add(GameEvent.attack(attacker.getId().toString(), "ENEMY_HERO",
					attacker.getCard().getName() + " attacks hero for " + damage));

			if (attacker.getKeywords() != null && attacker.getKeywords().contains(Keyword.LIFESTEAL)) {
				spellResolver.healHero(attackerState, damage, events,
						attacker.getCard().getName() + " (Lifesteal)");
			}
		} else {
			BoardCreature defender = defenderState.getBattlefield().stream()
				.filter(c -> c.getId() != null && c.getId().toString().equals(targetId))
				.findFirst()
				.orElse(null);

			if (defender == null) return null;
			if (enemyHasGuard && (defender.getKeywords() == null || !defender.getKeywords().contains(Keyword.GUARD))) {
				return null;
			}

			events.add(GameEvent.attack(attacker.getId().toString(), defender.getId().toString(),
					attacker.getCard().getName() + " attacks " + defender.getCard().getName()));

			int attackerDmg = attacker.getAttack();
			int defenderDmg = defender.getAttack();

			int actualToDefender = spellResolver.applyDamageToCreature(defender, attackerDmg, events,
					attacker.getCard().getName());
			int actualToAttacker = spellResolver.applyDamageToCreature(attacker, defenderDmg, events,
					defender.getCard().getName());

			attacker.setHasAttackedThisTurn(true);

			if (attacker.getKeywords() != null && attacker.getKeywords().contains(Keyword.LIFESTEAL)
					&& actualToDefender > 0) {
				spellResolver.healHero(attackerState, actualToDefender, events,
						attacker.getCard().getName() + " (Lifesteal)");
			}
			if (defender.getKeywords() != null && defender.getKeywords().contains(Keyword.LIFESTEAL)
					&& actualToAttacker > 0) {
				spellResolver.healHero(defenderState, actualToAttacker, events,
						defender.getCard().getName() + " (Lifesteal)");
			}

			List<BoardCreature> dead = new ArrayList<>();
			if (defender.getHealth() <= 0) dead.add(defender);
			if (attacker.getHealth() <= 0) dead.add(attacker);

			for (BoardCreature deadCreature : dead) {
				GamePlayerState owner = attackerState.getBattlefield().contains(deadCreature)
						? attackerState : defenderState;
				moveCreatureToDiscard(owner, deadCreature);
				spellResolver.killCreature(owner, deadCreature, events);
				events.addAll(creatureResolver.resolveWhenDies(deadCreature, owner,
						owner == attackerState ? defenderState : attackerState));
			}
		}

		return new GameActionResponse(null, events);
	}

	// ==================================================================
	// INTERNAL HELPERS
	// ==================================================================

	/**
	 * Process start of a player's turn: increment mana, refill, ready creatures, draw card.
	 */
	List<GameEvent> processStartOfTurn(Game game, GamePlayerState state) {
		List<GameEvent> events = new ArrayList<>();

		events.add(GameEvent.turnStart(state.getUserId(), game.getTurnNumber(),
				"Turn " + game.getTurnNumber() + " for " + state.getUserId()));

		// Increment max mana (cap at 10)
		if (state.getMaxMana() < 10) {
			state.setMaxMana(state.getMaxMana() + 1);
		}
		// Refill current mana
		state.setCurrentMana(state.getMaxMana());
		events.add(GameEvent.manaGain(state.getUserId(), state.getMaxMana(),
				"Mana: " + state.getMaxMana() + "/" + state.getMaxMana()));

		// Ready creatures and handle Frozen
		for (BoardCreature creature : state.getBattlefield()) {
			if (creature.getStatuses() != null && creature.getStatuses().contains(Status.FROZEN)) {
				// Frozen prevents attacking this turn, then clears
				creature.getStatuses().remove(Status.FROZEN);
				creature.setCanAttack(false);
			} else {
				creature.setCanAttack(true);
			}
			creature.setHasAttackedThisTurn(false);
		}

		// Start-of-turn creature effects
		GamePlayerState opponentState = state == game.getPlayer1State()
				? game.getPlayer2State() : game.getPlayer1State();
		events.addAll(creatureResolver.resolveStartOfTurn(state, opponentState));

		// Draw a card
		spellResolver.drawCard(state, events);

		return events;
	}

	private Game loadGame(UUID gameId) {
		return gameRepository.findById(gameId)
			.orElseThrow(() -> new IllegalArgumentException("Game not found"));
	}

	private void validatePlayerTurn(Game game, String playerId) {
		if (game.getGameStatus() == GameStatus.FINISHED) {
			throw new IllegalArgumentException("Game is already finished");
		}
		if (!game.getCurrentPlayerId().equals(playerId)) {
			throw new IllegalArgumentException("Not your turn");
		}
	}

	private void validatePhase(Game game, GamePhase expected) {
		if (game.getCurrentPhase() != expected) {
			throw new IllegalArgumentException("Invalid phase: expected " + expected);
		}
	}

	GamePlayerState getPlayerState(Game game, String playerId) {
		if (game.getPlayer1Id().equals(playerId)) return game.getPlayer1State();
		if (game.getPlayer2Id().equals(playerId)) return game.getPlayer2State();
		throw new IllegalArgumentException("Player not in game");
	}

	GamePlayerState getOpponentState(Game game, String playerId) {
		if (game.getPlayer1Id().equals(playerId)) return game.getPlayer2State();
		if (game.getPlayer2Id().equals(playerId)) return game.getPlayer1State();
		throw new IllegalArgumentException("Player not in game");
	}

	private List<GameEvent> checkGameOver(Game game) {
		List<GameEvent> events = new ArrayList<>();

		if (game.getPlayer1State().getHeroHealth() <= 0) {
			game.setGameStatus(GameStatus.FINISHED);
			game.setWinnerId(game.getPlayer2Id());
			events.add(GameEvent.gameOver(game.getPlayer2Id(), "Player 1 hero defeated!"));
		} else if (game.getPlayer2State().getHeroHealth() <= 0) {
			game.setGameStatus(GameStatus.FINISHED);
			game.setWinnerId(game.getPlayer1Id());
			events.add(GameEvent.gameOver(game.getPlayer1Id(), "Player 2 hero defeated!"));
		}

		return events;
	}

	private void initializePlayerState(GamePlayerState state, String userId, Deck deck, boolean goesFirst) {
		state.setUserId(userId);
		state.setDeckId(deck.getId().toString());
		state.setHeroHealth(25);
		state.setMaxMana(0); // Will become 1 at start of turn 1
		state.setCurrentMana(0);
		state.setFatigueCounter(0);

		List<Card> deckCards = buildDeckList(deck);
		Collections.shuffle(deckCards, random);

		int initialHandSize = goesFirst ? 3 : 4;

		int position = 0;
		for (int i = 0; i < initialHandSize && i < deckCards.size(); i++) {
			PlayerZoneCard zoneCard = new PlayerZoneCard(state, deckCards.get(i), CardZone.HAND, position++);
			state.getZoneCards().add(zoneCard);
		}

		position = 0;
		for (int i = initialHandSize; i < deckCards.size(); i++) {
			PlayerZoneCard zoneCard = new PlayerZoneCard(state, deckCards.get(i), CardZone.DECK, position++);
			state.getZoneCards().add(zoneCard);
		}

		state.setBattlefield(new ArrayList<>());
	}

	private List<Card> buildDeckList(Deck deck) {
		List<Card> cardList = new ArrayList<>();
		for (DeckCard deckCard : deck.getDeckCards()) {
			for (int i = 0; i < deckCard.getQuantity(); i++) {
				cardList.add(deckCard.getCard());
			}
		}
		return cardList;
	}

	private void moveCreatureToDiscard(GamePlayerState owner, BoardCreature creature) {
		long maxPos = owner.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.DISCARD)
			.mapToInt(PlayerZoneCard::getPosition)
			.max()
			.orElse(-1);
		PlayerZoneCard discard = new PlayerZoneCard(owner, creature.getCard(), CardZone.DISCARD, (int) maxPos + 1);
		owner.getZoneCards().add(discard);
	}

	private Faction pickDifferentFaction(Faction exclude) {
		Faction[] all = Faction.values();
		List<Faction> choices = new ArrayList<>();
		for (Faction f : all) {
			if (f != exclude) choices.add(f);
		}
		return choices.get(random.nextInt(choices.size()));
	}

	private MagicSchool pickDifferentSchool(MagicSchool exclude) {
		MagicSchool[] all = MagicSchool.values();
		List<MagicSchool> choices = new ArrayList<>();
		for (MagicSchool s : all) {
			if (s != exclude) choices.add(s);
		}
		return choices.get(random.nextInt(choices.size()));
	}

	/**
	 * Simple target picker for AI spells.
	 */
	String pickAiSpellTarget(Card spellCard, GamePlayerState aiState, GamePlayerState humanState) {
		String name = spellCard.getName();

		return switch (name) {
			// Damage to any target -> prefer enemy hero
			case "Ember Bolt", "Dark Touch" -> {
				if (!humanState.getBattlefield().isEmpty()) {
					// Pick weakest enemy creature
					BoardCreature weakest = humanState.getBattlefield().stream()
						.min((a, b) -> Integer.compare(a.getHealth(), b.getHealth()))
						.orElse(null);
					if (weakest != null && weakest.getHealth() <= (name.equals("Ember Bolt") ? 2 : 1)) {
						yield weakest.getId().toString();
					}
				}
				yield "ENEMY_HERO";
			}
			// Damage to creature
			case "Flame Javelin", "Ice Shard", "Vine Whip", "Arc Sparkbot" -> {
				if (!humanState.getBattlefield().isEmpty()) {
					yield humanState.getBattlefield().get(0).getId().toString();
				}
				yield null;
			}
			// Damage to creature (Combust)
			case "Combust" -> {
				if (!humanState.getBattlefield().isEmpty()) {
					yield humanState.getBattlefield().get(0).getId().toString();
				}
				yield null;
			}
			// Freeze enemy creature
			case "Glacial Binding" -> {
				if (!humanState.getBattlefield().isEmpty()) {
					BoardCreature strongest = humanState.getBattlefield().stream()
						.max((a, b) -> Integer.compare(a.getAttack(), b.getAttack()))
						.orElse(null);
					yield strongest != null ? strongest.getId().toString() : null;
				}
				yield null;
			}
			// Shatter frozen creature
			case "Shatter" -> {
				BoardCreature frozen = humanState.getBattlefield().stream()
					.filter(c -> c.getStatuses() != null && c.getStatuses().contains(Status.FROZEN))
					.findFirst().orElse(null);
				yield frozen != null ? frozen.getId().toString() : null;
			}
			// Buff friendly creature
			case "Frost Shield", "Growth" -> {
				if (!aiState.getBattlefield().isEmpty()) {
					yield aiState.getBattlefield().get(0).getId().toString();
				}
				yield null;
			}
			// Wither enemy creature
			case "Wither" -> {
				if (!humanState.getBattlefield().isEmpty()) {
					BoardCreature strongest = humanState.getBattlefield().stream()
						.max((a, b) -> Integer.compare(a.getAttack(), b.getAttack()))
						.orElse(null);
					yield strongest != null ? strongest.getId().toString() : null;
				}
				yield null;
			}
			// Grim Bargain: sacrifice weakest own creature
			case "Grim Bargain" -> {
				if (!aiState.getBattlefield().isEmpty()) {
					BoardCreature weakest = aiState.getBattlefield().stream()
						.min((a, b) -> Integer.compare(a.getAttack() + a.getHealth(), b.getAttack() + b.getHealth()))
						.orElse(null);
					yield weakest != null ? weakest.getId().toString() : null;
				}
				yield null;
			}
			// Void Snare: destroy enemy creature cost ≤ 5
			case "Void Snare" -> {
				BoardCreature target = humanState.getBattlefield().stream()
					.filter(c -> c.getCard().getCost() <= 5)
					.max((a, b) -> Integer.compare(a.getCard().getCost(), b.getCard().getCost()))
					.orElse(null);
				yield target != null ? target.getId().toString() : null;
			}
			// Bone Acolyte: 1 damage to any target
			case "Bone Acolyte" -> {
				if (!humanState.getBattlefield().isEmpty()) {
					BoardCreature weakest = humanState.getBattlefield().stream()
						.filter(c -> c.getHealth() == 1)
						.findFirst().orElse(null);
					if (weakest != null) yield weakest.getId().toString();
				}
				yield "ENEMY_HERO";
			}
			// No target needed
			default -> null;
		};
	}

	/**
	 * Get the game entity by ID for internal use.
	 */
	Game getGameEntity(UUID gameId) {
		return loadGame(gameId);
	}
}
