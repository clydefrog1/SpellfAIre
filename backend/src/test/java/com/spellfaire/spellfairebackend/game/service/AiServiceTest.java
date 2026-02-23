package com.spellfaire.spellfairebackend.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.spellfaire.spellfairebackend.game.model.GamePlayerState;
import com.spellfaire.spellfairebackend.game.model.Keyword;
import com.spellfaire.spellfairebackend.game.model.PlayerZoneCard;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

	@Mock
	private GameplayService gameplayService;

	private AiService aiService;

	@BeforeEach
	void setUp() {
		aiService = new AiService(gameplayService);
	}

	@Test
	void executeTurnUsesLethalBurnAndStopsBeforeAttacksWhenHeroFalls() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");
		humanState.setHeroHealth(2);

		Card emberBolt = spell("Ember Bolt", 1);
		aiState.setCurrentMana(10);
		aiState.getZoneCards().add(new PlayerZoneCard(aiState, emberBolt, CardZone.HAND, 0));

		BoardCreature attacker = creature(aiState, "Attacker", 4, 4, 2, Set.of());
		attacker.setCanAttack(true);
		aiState.getBattlefield().add(attacker);

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);
		when(gameplayService.playCardInternal(any(Game.class), any(String.class), any(PlayCardRequest.class)))
			.thenAnswer(invocation -> {
				humanState.setHeroHealth(0);
				return new GameActionResponse(null, List.of(GameEvent.spellResolved("spell", "burn")));
			});

		List<GameEvent> events = aiService.executeTurn(game);

		ArgumentCaptor<PlayCardRequest> playCaptor = ArgumentCaptor.forClass(PlayCardRequest.class);
		verify(gameplayService).playCardInternal(any(Game.class), any(String.class), playCaptor.capture());
		assertEquals("ENEMY_HERO", playCaptor.getValue().getTargetId());
		verify(gameplayService, never()).attackInternal(any(Game.class), any(String.class), any(AttackRequest.class));
		assertFalse(events.isEmpty());
	}

	@Test
	void executeTurnAttacksGuardCreatureWhenGuardExists() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");

		BoardCreature attacker = creature(aiState, "Attacker", 3, 3, 2, Set.of());
		attacker.setCanAttack(true);
		aiState.getBattlefield().add(attacker);

		BoardCreature guard = creature(humanState, "Guard", 1, 2, 2, Set.of(Keyword.GUARD));
		BoardCreature nonGuard = creature(humanState, "Non Guard", 1, 1, 1, Set.of());
		humanState.getBattlefield().add(guard);
		humanState.getBattlefield().add(nonGuard);

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);
		when(gameplayService.attackInternal(any(Game.class), any(String.class), any(AttackRequest.class)))
			.thenReturn(new GameActionResponse(null, new ArrayList<>()));

		aiService.executeTurn(game);

		ArgumentCaptor<AttackRequest> attackCaptor = ArgumentCaptor.forClass(AttackRequest.class);
		verify(gameplayService).attackInternal(any(Game.class), any(String.class), attackCaptor.capture());
		assertEquals(guard.getId().toString(), attackCaptor.getValue().getTargetId());
	}

	@Test
	void executeTurnPrefersValueTradeOverFaceWhenNoGuard() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");
		humanState.setHeroHealth(25);

		BoardCreature attacker = creature(aiState, "Attacker", 4, 5, 2, Set.of());
		attacker.setCanAttack(true);
		aiState.getBattlefield().add(attacker);

		BoardCreature highValueTarget = creature(humanState, "Value Target", 3, 4, 5, Set.of());
		humanState.getBattlefield().add(highValueTarget);

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);
		when(gameplayService.attackInternal(any(Game.class), any(String.class), any(AttackRequest.class)))
			.thenReturn(new GameActionResponse(null, new ArrayList<>()));

		aiService.executeTurn(game);

		ArgumentCaptor<AttackRequest> attackCaptor = ArgumentCaptor.forClass(AttackRequest.class);
		verify(gameplayService).attackInternal(any(Game.class), any(String.class), attackCaptor.capture());
		assertEquals(highValueTarget.getId().toString(), attackCaptor.getValue().getTargetId());
	}

	@Test
	void executeTurnGoesFaceWhenNoCreaturesToTrade() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");

		BoardCreature attacker = creature(aiState, "Attacker", 3, 3, 2, Set.of());
		attacker.setCanAttack(true);
		aiState.getBattlefield().add(attacker);

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);
		when(gameplayService.attackInternal(any(Game.class), any(String.class), any(AttackRequest.class)))
			.thenReturn(new GameActionResponse(null, new ArrayList<>()));

		List<GameEvent> events = aiService.executeTurn(game);

		ArgumentCaptor<AttackRequest> attackCaptor = ArgumentCaptor.forClass(AttackRequest.class);
		verify(gameplayService).attackInternal(any(Game.class), any(String.class), attackCaptor.capture());
		assertEquals("ENEMY_HERO", attackCaptor.getValue().getTargetId());
		assertNotNull(events);
	}

	@Test
	void executeTurnSkipsFrozenAndAlreadyAttackedCreatures() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");

		BoardCreature frozen = creature(aiState, "Frozen", 3, 3, 2, Set.of());
		frozen.setCanAttack(true);
		frozen.setFrozenBlocksAttacksThisTurn(true);
		BoardCreature spent = creature(aiState, "Spent", 3, 3, 2, Set.of());
		spent.setCanAttack(true);
		spent.setHasAttackedThisTurn(true);
		aiState.getBattlefield().add(frozen);
		aiState.getBattlefield().add(spent);

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);

		aiService.executeTurn(game);

		verify(gameplayService, never()).attackInternal(any(Game.class), any(String.class), any(AttackRequest.class));
	}

	@Test
	void executeTurnPrefersTradeUpWhenNoValueTradeExists() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");

		BoardCreature attacker = creature(aiState, "Attacker", 4, 3, 2, Set.of());
		attacker.setCanAttack(true);
		aiState.getBattlefield().add(attacker);

		BoardCreature tradeUpTarget = creature(humanState, "Trade Up", 6, 4, 7, Set.of());
		humanState.getBattlefield().add(tradeUpTarget);

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);
		when(gameplayService.attackInternal(any(Game.class), any(String.class), any(AttackRequest.class)))
			.thenReturn(new GameActionResponse(null, new ArrayList<>()));

		aiService.executeTurn(game);

		ArgumentCaptor<AttackRequest> attackCaptor = ArgumentCaptor.forClass(AttackRequest.class);
		verify(gameplayService, times(1)).attackInternal(any(Game.class), any(String.class), attackCaptor.capture());
		assertEquals(tradeUpTarget.getId().toString(), attackCaptor.getValue().getTargetId());
	}

	@Test
	void executeTurnPlayPhaseSkipsCreatureWhenBattlefieldFullAndPlaysSpell() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");

		for (int i = 0; i < 6; i++) {
			BoardCreature fullSlot = creature(aiState, "Filled " + i, 1, 1, 1, Set.of());
			fullSlot.setCanAttack(false);
			aiState.getBattlefield().add(fullSlot);
		}

		Card creatureCard = spellAsCreature("Big Creature", 8);
		Card spellCard = spell("Dark Touch", 1);
		aiState.getZoneCards().add(new PlayerZoneCard(aiState, creatureCard, CardZone.HAND, 0));
		aiState.getZoneCards().add(new PlayerZoneCard(aiState, spellCard, CardZone.HAND, 1));

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);
		when(gameplayService.playCardInternal(any(Game.class), any(String.class), any(PlayCardRequest.class)))
				.thenAnswer(invocation -> {
					PlayCardRequest req = invocation.getArgument(2);
					String playedId = req.getCardId();
					aiState.getZoneCards().stream()
							.filter(c -> c.getZone() == CardZone.HAND)
							.filter(c -> c.getCard().getId().toString().equals(playedId))
							.findFirst()
							.ifPresent(c -> c.setZone(CardZone.DISCARD));
					aiState.setCurrentMana(0);
					return new GameActionResponse(null, List.of(GameEvent.spellResolved("s", "played")));
				});

		aiService.executeTurn(game);

		ArgumentCaptor<PlayCardRequest> playCaptor = ArgumentCaptor.forClass(PlayCardRequest.class);
		verify(gameplayService, times(1)).playCardInternal(any(Game.class), any(String.class), playCaptor.capture());
		assertEquals(spellCard.getId().toString(), playCaptor.getValue().getCardId());
	}

	@Test
	void executeTurnPlayPhaseRetriesAfterNullThenPlaysNextCard() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");

		Card highCost = spell("Combust", 4);
		Card lowCost = spell("Ember Bolt", 1);
		aiState.getZoneCards().add(new PlayerZoneCard(aiState, highCost, CardZone.HAND, 0));
		aiState.getZoneCards().add(new PlayerZoneCard(aiState, lowCost, CardZone.HAND, 1));

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);
		when(gameplayService.playCardInternal(any(Game.class), any(String.class), any(PlayCardRequest.class)))
				.thenAnswer(invocation -> {
					PlayCardRequest req = invocation.getArgument(2);
					if (req.getCardId().equals(highCost.getId().toString())) {
						return null;
					}
					aiState.getZoneCards().stream()
							.filter(c -> c.getZone() == CardZone.HAND)
							.filter(c -> c.getCard().getId().toString().equals(lowCost.getId().toString()))
							.findFirst()
							.ifPresent(c -> c.setZone(CardZone.DISCARD));
					aiState.setCurrentMana(0);
					return new GameActionResponse(null, List.of(GameEvent.spellResolved("s", "played low")));
				});

		aiService.executeTurn(game);

		ArgumentCaptor<PlayCardRequest> playCaptor = ArgumentCaptor.forClass(PlayCardRequest.class);
		verify(gameplayService, times(2)).playCardInternal(any(Game.class), any(String.class), playCaptor.capture());
		List<PlayCardRequest> requests = playCaptor.getAllValues();
		assertEquals(highCost.getId().toString(), requests.get(0).getCardId());
		assertEquals(lowCost.getId().toString(), requests.get(1).getCardId());
	}

	@Test
	void executeTurnRecalculatesGuardAfterFirstAttack() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");

		BoardCreature attackerOne = creature(aiState, "Attacker 1", 3, 3, 2, Set.of());
		attackerOne.setCanAttack(true);
		BoardCreature attackerTwo = creature(aiState, "Attacker 2", 3, 3, 2, Set.of());
		attackerTwo.setCanAttack(true);
		aiState.getBattlefield().add(attackerOne);
		aiState.getBattlefield().add(attackerTwo);

		BoardCreature guard = creature(humanState, "Guard", 1, 2, 2, Set.of(Keyword.GUARD));
		humanState.getBattlefield().add(guard);

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);
		when(gameplayService.attackInternal(any(Game.class), any(String.class), any(AttackRequest.class)))
				.thenAnswer(invocation -> {
					AttackRequest req = invocation.getArgument(2);
					if (req.getTargetId().equals(guard.getId().toString())) {
						humanState.getBattlefield().remove(guard);
					}
					return new GameActionResponse(null, List.of(GameEvent.attack("a", req.getTargetId(), "attack")));
				});

		aiService.executeTurn(game);

		ArgumentCaptor<AttackRequest> attackCaptor = ArgumentCaptor.forClass(AttackRequest.class);
		verify(gameplayService, times(2)).attackInternal(any(Game.class), any(String.class), attackCaptor.capture());
		List<AttackRequest> attacks = attackCaptor.getAllValues();
		assertEquals(guard.getId().toString(), attacks.get(0).getTargetId());
		assertEquals("ENEMY_HERO", attacks.get(1).getTargetId());
	}

	@Test
	void executeTurnSkipsLethalWhenBurnSpellTooExpensiveThenAttacks() {
		Game game = new Game();
		GamePlayerState aiState = state("AI");
		GamePlayerState humanState = state("human");
		humanState.setHeroHealth(6);
		aiState.setCurrentMana(3);

		Card finalSpark = spell("Final Spark", 6);
		aiState.getZoneCards().add(new PlayerZoneCard(aiState, finalSpark, CardZone.HAND, 0));

		BoardCreature attacker = creature(aiState, "Attacker", 4, 4, 2, Set.of());
		attacker.setCanAttack(true);
		aiState.getBattlefield().add(attacker);

		when(gameplayService.getPlayerState(game, "AI")).thenReturn(aiState);
		when(gameplayService.getOpponentState(game, "AI")).thenReturn(humanState);
		when(gameplayService.attackInternal(any(Game.class), any(String.class), any(AttackRequest.class)))
				.thenReturn(new GameActionResponse(null, List.of(GameEvent.attack("a", "ENEMY_HERO", "face"))));

		aiService.executeTurn(game);

		verify(gameplayService, never()).playCardInternal(any(Game.class), any(String.class), any(PlayCardRequest.class));
		verify(gameplayService, times(1)).attackInternal(any(Game.class), any(String.class), any(AttackRequest.class));
	}

	private static Card spellAsCreature(String name, int cost) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCost(cost);
		card.setCardType(CardType.CREATURE);
		card.setFaction(Faction.KINGDOM);
		card.setAttack(6);
		card.setHealth(6);
		return card;
	}

	private static GamePlayerState state(String userId) {
		GamePlayerState state = new GamePlayerState();
		state.setUserId(userId);
		state.setHeroHealth(25);
		state.setCurrentMana(10);
		return state;
	}

	private static Card spell(String name, int cost) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCost(cost);
		card.setCardType(CardType.SPELL);
		return card;
	}

	private static BoardCreature creature(GamePlayerState owner, String name, int attack, int health, int cost,
										 Set<Keyword> keywords) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.CREATURE);
		card.setFaction(Faction.KINGDOM);
		card.setCost(cost);
		card.setAttack(attack);
		card.setHealth(health);

		BoardCreature creature = new BoardCreature(owner, card, attack, health, new HashSet<>(keywords),
				owner.getBattlefield().size());
		creature.setId(UUID.randomUUID());
		return creature;
	}
}