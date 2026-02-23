package com.spellfaire.spellfairebackend.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spellfaire.spellfairebackend.auth.repo.UserRepository;
import com.spellfaire.spellfairebackend.game.dto.AttackRequest;
import com.spellfaire.spellfairebackend.game.dto.GameActionResponse;
import com.spellfaire.spellfairebackend.game.dto.GameEvent;
import com.spellfaire.spellfairebackend.game.dto.PlayCardRequest;
import com.spellfaire.spellfairebackend.game.model.BoardCreature;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.CardZone;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.Game;
import com.spellfaire.spellfairebackend.game.model.GamePhase;
import com.spellfaire.spellfairebackend.game.model.GamePlayerState;
import com.spellfaire.spellfairebackend.game.model.GameStatus;
import com.spellfaire.spellfairebackend.game.model.Keyword;
import com.spellfaire.spellfairebackend.game.model.PlayerZoneCard;
import com.spellfaire.spellfairebackend.game.model.Status;
import com.spellfaire.spellfairebackend.game.repo.GameRepository;

@ExtendWith(MockitoExtension.class)
class GameplayServiceTest {

	@Mock
	private GameRepository gameRepository;

	@Mock
	private GameService gameService;

	@Mock
	private DeckService deckService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private SpellEffectResolver spellResolver;

	@Mock
	private CreatureEffectResolver creatureResolver;

	@Mock
	private AiService aiService;

	private GameplayService gameplayService;

	@BeforeEach
	void setUp() {
		gameplayService = new GameplayService(
				gameRepository,
				gameService,
				deckService,
				userRepository,
				spellResolver,
				creatureResolver,
				aiService);
	}

	@Test
	void attackInternalBlocksHeroAttackWhenGuardExists() {
		Game game = baseGame();
		GamePlayerState attackerState = game.getPlayer1State();
		GamePlayerState defenderState = game.getPlayer2State();

		BoardCreature attacker = creature(attackerState, "Attacker", 4, 3, Set.of());
		attacker.setCanAttack(true);
		attackerState.getBattlefield().add(attacker);

		BoardCreature guard = creature(defenderState, "Guard", 1, 5, Set.of(Keyword.GUARD));
		defenderState.getBattlefield().add(guard);

		AttackRequest request = new AttackRequest();
		request.setAttackerInstanceId(attacker.getId().toString());
		request.setTargetId("ENEMY_HERO");

		GameActionResponse response = gameplayService.attackInternal(game, game.getPlayer1Id(), request);

		assertNull(response);
		assertEquals(25, defenderState.getHeroHealth());
		assertFalse(attacker.isHasAttackedThisTurn());
	}

	@Test
	void processStartOfTurnConsumesFrozenForOneTurnAndRefillsMana() {
		Game game = baseGame();
		game.setTurnNumber(5);

		GamePlayerState playerState = game.getPlayer1State();
		playerState.setMaxMana(9);
		playerState.setCurrentMana(0);

		BoardCreature frozenCreature = creature(playerState, "Frozen Unit", 2, 2, Set.of());
		frozenCreature.setStatuses(new HashSet<>(Set.of(Status.FROZEN)));
		frozenCreature.setFrozenForNextTurn(true);
		frozenCreature.setCanAttack(true);
		frozenCreature.setHasAttackedThisTurn(true);

		BoardCreature normalCreature = creature(playerState, "Normal Unit", 3, 3, Set.of());
		normalCreature.setStatuses(new HashSet<>());
		normalCreature.setCanAttack(false);
		normalCreature.setHasAttackedThisTurn(true);

		playerState.getBattlefield().add(frozenCreature);
		playerState.getBattlefield().add(normalCreature);

		when(creatureResolver.resolveStartOfTurn(playerState, game.getPlayer2State()))
				.thenReturn(Collections.emptyList());

		var events = gameplayService.processStartOfTurn(game, playerState);

		assertEquals(10, playerState.getMaxMana());
		assertEquals(10, playerState.getCurrentMana());
		assertFalse(frozenCreature.isFrozenForNextTurn());
		assertTrue(frozenCreature.isFrozenBlocksAttacksThisTurn());
		assertFalse(frozenCreature.getStatuses().contains(Status.FROZEN));
		assertFalse(frozenCreature.isCanAttack());
		assertFalse(frozenCreature.isHasAttackedThisTurn());

		game.setTurnNumber(6);
		var secondTurnEvents = gameplayService.processStartOfTurn(game, playerState);
		assertFalse(frozenCreature.isFrozenBlocksAttacksThisTurn());
		assertTrue(frozenCreature.isCanAttack());
		assertNotNull(secondTurnEvents);

		assertTrue(normalCreature.isCanAttack());
		assertFalse(normalCreature.isHasAttackedThisTurn());
		assertNotNull(events);
		assertTrue(events.size() >= 2);

		verify(spellResolver, times(2)).drawCard(eq(playerState), anyList());
	}

	@Test
	void attackInternalAllowsSameTurnAttackWhenFrozenIsQueuedForNextTurn() {
		Game game = baseGame();
		GamePlayerState attackerState = game.getPlayer1State();
		GamePlayerState defenderState = game.getPlayer2State();

		BoardCreature attacker = creature(attackerState, "Queued Frozen Attacker", 4, 3, Set.of());
		attacker.setCanAttack(true);
		attacker.setHasAttackedThisTurn(false);
		attacker.setStatuses(new HashSet<>(Set.of(Status.FROZEN)));
		attacker.setFrozenForNextTurn(true);
		attacker.setFrozenBlocksAttacksThisTurn(false);
		attackerState.getBattlefield().add(attacker);

		BoardCreature defender = creature(defenderState, "Training Dummy", 1, 5, Set.of());
		defenderState.getBattlefield().add(defender);

		AttackRequest attackNow = new AttackRequest();
		attackNow.setAttackerInstanceId(attacker.getId().toString());
		attackNow.setTargetId(defender.getId().toString());

		GameActionResponse sameTurnAttack = gameplayService.attackInternal(game, game.getPlayer1Id(), attackNow);

		assertNotNull(sameTurnAttack);
		assertTrue(attacker.isHasAttackedThisTurn());

		attacker.setHasAttackedThisTurn(false);
		game.setTurnNumber(2);
		when(creatureResolver.resolveStartOfTurn(attackerState, defenderState)).thenReturn(Collections.emptyList());
		gameplayService.processStartOfTurn(game, attackerState);

		assertTrue(attacker.isFrozenBlocksAttacksThisTurn());
		assertFalse(attacker.isFrozenForNextTurn());
		assertFalse(attacker.isCanAttack());
		assertFalse(attacker.getStatuses().contains(Status.FROZEN));

		attacker.setCanAttack(true);
		AttackRequest blockedAttack = new AttackRequest();
		blockedAttack.setAttackerInstanceId(attacker.getId().toString());
		blockedAttack.setTargetId("ENEMY_HERO");

		GameActionResponse blockedTurnAttack = gameplayService.attackInternal(game, game.getPlayer1Id(), blockedAttack);
		assertNull(blockedTurnAttack);
	}

	@Test
	void pickAiSpellTargetPrefersFrozenCreatureForShatter() {
		GamePlayerState aiState = new GamePlayerState();
		aiState.setUserId("AI");

		GamePlayerState humanState = new GamePlayerState();
		humanState.setUserId(UUID.randomUUID().toString());

		BoardCreature frozenTarget = creature(humanState, "Frozen Target", 2, 4, Set.of());
		frozenTarget.setStatuses(new HashSet<>(Set.of(Status.FROZEN)));
		humanState.getBattlefield().add(frozenTarget);

		Card shatter = new Card();
		shatter.setId(UUID.randomUUID());
		shatter.setName("Shatter");
		shatter.setCardType(CardType.SPELL);

		String selectedTarget = gameplayService.pickAiSpellTarget(shatter, aiState, humanState);

		assertEquals(frozenTarget.getId().toString(), selectedTarget);
	}

	@Test
	void playCardRejectsInvalidVoidSnareTargetWithoutSpendingMana() {
		Game game = baseGame();
		game.setId(UUID.randomUUID());
		game.setGameStatus(GameStatus.IN_PROGRESS);
		game.setCurrentPhase(GamePhase.MAIN);

		GamePlayerState casterState = game.getPlayer1State();
		GamePlayerState opponentState = game.getPlayer2State();
		casterState.setCurrentMana(10);

		Card voidSnare = new Card();
		voidSnare.setId(UUID.randomUUID());
		voidSnare.setName("Void Snare");
		voidSnare.setCardType(CardType.SPELL);
		voidSnare.setCost(6);
		PlayerZoneCard handSpell = new PlayerZoneCard(casterState, voidSnare, CardZone.HAND, 0);
		casterState.getZoneCards().add(handSpell);

		BoardCreature expensiveEnemy = creature(opponentState, "Titan Forgeguard", 7, 9, Set.of(Keyword.GUARD));
		expensiveEnemy.getCard().setCost(8);
		opponentState.getBattlefield().add(expensiveEnemy);

		PlayCardRequest request = new PlayCardRequest();
		request.setCardId(voidSnare.getId().toString());
		request.setTargetId(expensiveEnemy.getId().toString());

		when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

		assertThrows(IllegalArgumentException.class,
				() -> gameplayService.playCard(game.getId(), game.getPlayer1Id(), request));
		assertEquals(10, casterState.getCurrentMana());
		assertEquals(CardZone.HAND, handSpell.getZone());
	}

	@Test
	void playCardSpellKillMovesCreatureToDiscardAndResolvesDeathTrigger() {
		Game game = baseGame();
		game.setId(UUID.randomUUID());
		game.setGameStatus(GameStatus.IN_PROGRESS);
		game.setCurrentPhase(GamePhase.MAIN);

		GamePlayerState casterState = game.getPlayer1State();
		GamePlayerState opponentState = game.getPlayer2State();
		casterState.setCurrentMana(10);

		Card darkTouch = new Card();
		darkTouch.setId(UUID.randomUUID());
		darkTouch.setName("Dark Touch");
		darkTouch.setCardType(CardType.SPELL);
		darkTouch.setCost(1);
		PlayerZoneCard handSpell = new PlayerZoneCard(casterState, darkTouch, CardZone.HAND, 0);
		casterState.getZoneCards().add(handSpell);

		BoardCreature graveRat = creature(opponentState, "Grave Rat", 1, 1, Set.of());
		opponentState.getBattlefield().add(graveRat);

		PlayCardRequest request = new PlayCardRequest();
		request.setCardId(darkTouch.getId().toString());
		request.setTargetId(graveRat.getId().toString());

		when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
		when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(gameService.toGameResponse(any(Game.class))).thenReturn(null);
		when(spellResolver.resolveSpell(eq(darkTouch), eq(casterState), eq(opponentState), eq(graveRat.getId().toString())))
				.thenAnswer(invocation -> {
					opponentState.getBattlefield().remove(graveRat);
					return new ArrayList<GameEvent>();
				});
		when(creatureResolver.resolveWhenDies(eq(graveRat), eq(opponentState), eq(casterState)))
				.thenReturn(List.of(GameEvent.cardDrawn(opponentState.getUserId(), "Grave Rat trigger")));

		GameActionResponse response = gameplayService.playCard(game.getId(), game.getPlayer1Id(), request);

		assertNotNull(response);
		assertTrue(opponentState.getZoneCards().stream()
				.anyMatch(card -> card.getZone() == CardZone.DISCARD && card.getCard().getId().equals(graveRat.getCard().getId())));
		verify(creatureResolver).resolveWhenDies(eq(graveRat), eq(opponentState), eq(casterState));
	}

	@Test
	void processStartOfTurnClearsTemporaryAttackDebuff() {
		Game game = baseGame();
		game.setTurnNumber(2);

		GamePlayerState playerState = game.getPlayer1State();
		BoardCreature witheredCreature = creature(playerState, "Withered Unit", 1, 3, Set.of());
		witheredCreature.setTemporaryAttackDebuff(2);
		witheredCreature.setCanAttack(false);
		playerState.getBattlefield().add(witheredCreature);

		when(creatureResolver.resolveStartOfTurn(playerState, game.getPlayer2State()))
				.thenReturn(Collections.emptyList());

		gameplayService.processStartOfTurn(game, playerState);

		assertEquals(3, witheredCreature.getAttack());
		assertEquals(0, witheredCreature.getTemporaryAttackDebuff());
	}

	private static Game baseGame() {
		Game game = new Game();
		game.setPlayer1Id(UUID.randomUUID().toString());
		game.setPlayer2Id(UUID.randomUUID().toString());
		game.setCurrentPlayerId(game.getPlayer1Id());

		game.getPlayer1State().setUserId(game.getPlayer1Id());
		game.getPlayer2State().setUserId(game.getPlayer2Id());
		game.getPlayer1State().setHeroHealth(25);
		game.getPlayer2State().setHeroHealth(25);
		return game;
	}

	private static BoardCreature creature(GamePlayerState owner, String name, int attack, int health,
			Set<Keyword> keywords) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.CREATURE);
		card.setCost(2);
		card.setAttack(attack);
		card.setHealth(health);
		card.setFaction(Faction.KINGDOM);

		BoardCreature creature = new BoardCreature(owner, card, attack, health, new HashSet<>(keywords),
				owner.getBattlefield().size());
		creature.setId(UUID.randomUUID());
		return creature;
	}
}