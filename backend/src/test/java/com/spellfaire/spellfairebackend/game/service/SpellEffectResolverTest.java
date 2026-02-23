package com.spellfaire.spellfairebackend.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.spellfaire.spellfairebackend.game.dto.GameEvent;
import com.spellfaire.spellfairebackend.game.model.BoardCreature;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.CardZone;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.GamePlayerState;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;
import com.spellfaire.spellfairebackend.game.model.PlayerZoneCard;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;

class SpellEffectResolverTest {

	private SpellEffectResolver resolver;

	@BeforeEach
	void setUp() {
		CardRepository cardRepository = Mockito.mock(CardRepository.class);
		resolver = new SpellEffectResolver(cardRepository);
	}

	@Test
	void darkTouchDealsOneDamageToAnyTargetAndHealsCasterForOne() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");
		caster.setHeroHealth(20);

		BoardCreature target = creature(opponent, "Target", 3, 4, 3);
		opponent.getBattlefield().add(target);

		Card darkTouch = shadowSpell("Dark Touch", 1);
		List<GameEvent> events = resolver.resolveSpell(darkTouch, caster, opponent, target.getId().toString());

		assertEquals(3, target.getHealth());
		assertEquals(21, caster.getHeroHealth());
		assertTrue(events.stream().anyMatch(event -> event.getType() == GameEvent.EventType.DAMAGE));
		assertTrue(events.stream().anyMatch(event -> event.getType() == GameEvent.EventType.HEAL));
	}

	@Test
	void witherAppliesTemporaryTwoAttackReduction() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature enemy = creature(opponent, "Enemy", 5, 5, 4);
		opponent.getBattlefield().add(enemy);

		Card wither = shadowSpell("Wither", 2);
		List<GameEvent> events = resolver.resolveSpell(wither, caster, opponent, enemy.getId().toString());

		assertEquals(3, enemy.getAttack());
		assertEquals(2, enemy.getTemporaryAttackDebuff());
		assertTrue(events.stream().anyMatch(event -> event.getType() == GameEvent.EventType.BUFF));
	}

	@Test
	void grimBargainDestroysFriendlyCreatureAndDrawsTwoCards() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature friendly = creature(caster, "Sacrifice", 2, 2, 2);
		caster.getBattlefield().add(friendly);

		Card deckCardOne = creatureCard("Deck Creature One", 2, 2, 2);
		Card deckCardTwo = creatureCard("Deck Creature Two", 3, 3, 3);
		caster.getZoneCards().add(new PlayerZoneCard(caster, deckCardOne, CardZone.DECK, 0));
		caster.getZoneCards().add(new PlayerZoneCard(caster, deckCardTwo, CardZone.DECK, 1));

		Card grimBargain = shadowSpell("Grim Bargain", 4);
		List<GameEvent> events = resolver.resolveSpell(grimBargain, caster, opponent, friendly.getId().toString());

		assertFalse(caster.getBattlefield().contains(friendly));
		long handCount = caster.getZoneCards().stream().filter(card -> card.getZone() == CardZone.HAND).count();
		assertEquals(2, handCount);
		assertTrue(events.stream().anyMatch(event -> event.getType() == GameEvent.EventType.DEATH));
		assertTrue(events.stream().filter(event -> event.getType() == GameEvent.EventType.CARD_DRAWN).count() >= 2);
	}

	@Test
	void hauntingFogAppliesMinusOneMinusOneToAllEnemyCreatures() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature survivor = creature(opponent, "Survivor", 4, 4, 4);
		BoardCreature doomed = creature(opponent, "Doomed", 1, 1, 2);
		opponent.getBattlefield().add(survivor);
		opponent.getBattlefield().add(doomed);

		Card hauntingFog = shadowSpell("Haunting Fog", 5);
		List<GameEvent> events = resolver.resolveSpell(hauntingFog, caster, opponent, null);

		assertEquals(3, survivor.getAttack());
		assertEquals(3, survivor.getHealth());
		assertFalse(opponent.getBattlefield().contains(doomed));
		assertTrue(events.stream().filter(event -> event.getType() == GameEvent.EventType.BUFF).count() >= 2);
		assertTrue(events.stream().anyMatch(event -> event.getType() == GameEvent.EventType.DEATH));
	}

	@Test
	void voidSnareDestroysEnemyCreatureWithCostFiveOrLessOnly() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature lowCostTarget = creature(opponent, "Low Cost", 4, 4, 5);
		BoardCreature highCostTarget = creature(opponent, "High Cost", 6, 6, 6);
		opponent.getBattlefield().add(lowCostTarget);
		opponent.getBattlefield().add(highCostTarget);

		Card voidSnare = shadowSpell("Void Snare", 6);
		resolver.resolveSpell(voidSnare, caster, opponent, lowCostTarget.getId().toString());
		assertFalse(opponent.getBattlefield().contains(lowCostTarget));

		resolver.resolveSpell(voidSnare, caster, opponent, highCostTarget.getId().toString());
		assertTrue(opponent.getBattlefield().contains(highCostTarget));
	}

	private static GamePlayerState playerState(String userId) {
		GamePlayerState state = new GamePlayerState();
		state.setUserId(userId);
		state.setHeroHealth(25);
		return state;
	}

	private static Card shadowSpell(String name, int cost) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.SPELL);
		card.setCost(cost);
		card.setSchool(MagicSchool.SHADOW);
		return card;
	}

	private static Card creatureCard(String name, int cost, int attack, int health) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.CREATURE);
		card.setCost(cost);
		card.setAttack(attack);
		card.setHealth(health);
		card.setFaction(Faction.KINGDOM);
		return card;
	}

	private static BoardCreature creature(GamePlayerState owner, String name, int attack, int health, int cost) {
		Card card = creatureCard(name, cost, attack, health);
		BoardCreature creature = new BoardCreature(owner, card, attack, health, new HashSet<>(), owner.getBattlefield().size());
		creature.setId(UUID.randomUUID());
		return creature;
	}

}