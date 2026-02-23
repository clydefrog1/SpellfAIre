package com.spellfaire.spellfairebackend.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import com.spellfaire.spellfairebackend.game.model.Keyword;
import com.spellfaire.spellfairebackend.game.model.PlayerZoneCard;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;

class CreatureEffectResolverTest {

	private CreatureEffectResolver resolver;

	@BeforeEach
	void setUp() {
		CardRepository cardRepository = Mockito.mock(CardRepository.class);
		SpellEffectResolver spellHelper = new SpellEffectResolver(cardRepository);
		resolver = new CreatureEffectResolver(spellHelper);
	}

	@Test
	void squireCaptainBuffsChosenFriendlyCreatureHealth() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		BoardCreature target = creature(owner, "Town Guard", 1, 2, 1, Set.of(Keyword.GUARD));
		BoardCreature squireCaptain = creature(owner, "Squire Captain", 2, 2, 2, Set.of());
		owner.getBattlefield().add(target);
		owner.getBattlefield().add(squireCaptain);

		List<GameEvent> events = resolver.resolveWhenPlayed(
			squireCaptain,
			owner,
			opponent,
			target.getId().toString());

		assertEquals(3, target.getHealth());
		assertEquals(3, target.getMaxHealth());
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.BUFF));
	}

	@Test
	void frenziedMaulerDamagesOwnerAndGainsCharge() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");
		owner.setHeroHealth(20);

		BoardCreature mauler = creature(owner, "Frenzied Mauler", 5, 4, 5, Set.of());
		owner.getBattlefield().add(mauler);

		List<GameEvent> events = resolver.resolveWhenPlayed(mauler, owner, opponent, null);

		assertEquals(19, owner.getHeroHealth());
		assertTrue(mauler.isCanAttack());
		assertTrue(mauler.getKeywords().contains(Keyword.CHARGE));
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.DAMAGE));
	}

	@Test
	void bannerKnightGainsAttackWhenAnotherGuardExists() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		BoardCreature guard = creature(owner, "Town Guard", 1, 2, 1, Set.of(Keyword.GUARD));
		BoardCreature bannerKnight = creature(owner, "Banner Knight", 3, 3, 3, Set.of());
		owner.getBattlefield().add(guard);
		owner.getBattlefield().add(bannerKnight);

		resolver.resolveWhenPlayed(bannerKnight, owner, opponent, null);

		assertEquals(4, bannerKnight.getAttack());
	}

	@Test
	void alphaHowlerBuffsOtherFriendlyCreaturesOnly() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		BoardCreature alpha = creature(owner, "Alpha Howler", 2, 4, 3, Set.of());
		BoardCreature ally = creature(owner, "Ally", 2, 2, 2, Set.of());
		owner.getBattlefield().add(alpha);
		owner.getBattlefield().add(ally);

		resolver.resolveWhenPlayed(alpha, owner, opponent, null);

		assertEquals(2, alpha.getAttack());
		assertEquals(3, ally.getAttack());
	}

	@Test
	void boneAcolyteCanTargetFriendlyHero() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");
		owner.setHeroHealth(20);

		BoardCreature acolyte = creature(owner, "Bone Acolyte", 2, 2, 2, Set.of());
		owner.getBattlefield().add(acolyte);

		resolver.resolveWhenPlayed(acolyte, owner, opponent, "FRIENDLY_HERO");

		assertEquals(19, owner.getHeroHealth());
	}

	@Test
	void arcSparkbotKillsTargetedEnemyCreature() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		BoardCreature sparkbot = creature(owner, "Arc Sparkbot", 3, 3, 3, Set.of());
		BoardCreature enemy = creature(opponent, "Enemy", 1, 1, 1, Set.of());
		owner.getBattlefield().add(sparkbot);
		opponent.getBattlefield().add(enemy);

		resolver.resolveWhenPlayed(sparkbot, owner, opponent, enemy.getId().toString());

		assertFalse(opponent.getBattlefield().contains(enemy));
	}

	@Test
	void platingEngineerGivesWardToSelectedFriendlyCreature() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		BoardCreature engineer = creature(owner, "Plating Engineer", 2, 4, 3, Set.of());
		BoardCreature ally = creature(owner, "Ally", 2, 2, 2, Set.of());
		owner.getBattlefield().add(engineer);
		owner.getBattlefield().add(ally);

		resolver.resolveWhenPlayed(engineer, owner, opponent, ally.getId().toString());

		assertTrue(ally.getKeywords().contains(Keyword.WARD));
	}

	@Test
	void overclockColossusDealsSelfDamageAndGainsAttack() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");
		owner.setHeroHealth(25);

		BoardCreature colossus = creature(owner, "Overclock Colossus", 6, 7, 6, Set.of());
		owner.getBattlefield().add(colossus);

		resolver.resolveWhenPlayed(colossus, owner, opponent, null);

		assertEquals(23, owner.getHeroHealth());
		assertEquals(7, colossus.getAttack());
	}

	@Test
	void boneAcolyteCanKillEnemyCreature() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		BoardCreature acolyte = creature(owner, "Bone Acolyte", 2, 2, 2, Set.of());
		BoardCreature enemy = creature(opponent, "Enemy", 1, 1, 1, Set.of());
		owner.getBattlefield().add(acolyte);
		opponent.getBattlefield().add(enemy);

		List<GameEvent> events = resolver.resolveWhenPlayed(acolyte, owner, opponent, enemy.getId().toString());

		assertFalse(opponent.getBattlefield().contains(enemy));
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.DEATH));
	}

	@Test
	void graveRatDeathDrawsCardFromDeck() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		owner.getZoneCards().add(new PlayerZoneCard(owner, creatureCard("Deck Creature", 2, 2, 2), CardZone.DECK, 0));
		BoardCreature graveRat = creature(owner, "Grave Rat", 1, 2, 1, Set.of());

		List<GameEvent> events = resolver.resolveWhenDies(graveRat, owner, opponent);

		assertEquals(1, owner.getZoneCards().stream().filter(c -> c.getZone() == CardZone.HAND).count());
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.CARD_DRAWN));
	}

	@Test
	void soulCollectorDeathHealsOwnerHero() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");
		owner.setHeroHealth(20);

		BoardCreature soulCollector = creature(owner, "Soul Collector", 4, 3, 4, Set.of());

		resolver.resolveWhenDies(soulCollector, owner, opponent);

		assertEquals(23, owner.getHeroHealth());
	}

	@Test
	void lichAdeptStartOfTurnReturnsCheapDiscardCreatureToHand() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		BoardCreature lichAdept = creature(owner, "Lich Adept", 5, 7, 7, Set.of());
		owner.getBattlefield().add(lichAdept);

		Card cheapDiscardCreature = creatureCard("Grave Rat", 1, 1, 2);
		owner.getZoneCards().add(new PlayerZoneCard(owner, cheapDiscardCreature, CardZone.DISCARD, 0));
		owner.getZoneCards().add(new PlayerZoneCard(owner, spellCard("Mend", 1), CardZone.DISCARD, 1));

		List<GameEvent> events = resolver.resolveStartOfTurn(owner, opponent);

		assertEquals(1, owner.getZoneCards().stream().filter(c -> c.getZone() == CardZone.HAND).count());
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.CARD_DRAWN));
	}

	@Test
	void lichAdeptIgnoresNonCreatureAndHighCostDiscards() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		BoardCreature lichAdept = creature(owner, "Lich Adept", 5, 7, 7, Set.of());
		owner.getBattlefield().add(lichAdept);

		owner.getZoneCards().add(new PlayerZoneCard(owner, spellCard("Mend", 1), CardZone.DISCARD, 0));
		owner.getZoneCards().add(new PlayerZoneCard(owner, creatureCard("Expensive", 6, 6, 6), CardZone.DISCARD, 1));

		List<GameEvent> events = resolver.resolveStartOfTurn(owner, opponent);

		assertEquals(0, owner.getZoneCards().stream().filter(c -> c.getZone() == CardZone.HAND).count());
		assertTrue(events.isEmpty());
	}

	@Test
	void royalTacticianBuffsCreatureAtStartOfTurn() {
		GamePlayerState owner = state("owner");
		GamePlayerState opponent = state("opponent");

		BoardCreature tactician = creature(owner, "Royal Tactician", 4, 5, 5, Set.of());
		owner.getBattlefield().add(tactician);

		resolver.resolveStartOfTurn(owner, opponent);

		assertEquals(5, tactician.getAttack());
		assertEquals(6, tactician.getHealth());
		assertEquals(6, tactician.getMaxHealth());
	}

	private static GamePlayerState state(String userId) {
		GamePlayerState state = new GamePlayerState();
		state.setUserId(userId);
		state.setHeroHealth(25);
		return state;
	}

	private static Card creatureCard(String name, int cost, int attack, int health) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.CREATURE);
		card.setFaction(Faction.KINGDOM);
		card.setCost(cost);
		card.setAttack(attack);
		card.setHealth(health);
		return card;
	}

	private static Card spellCard(String name, int cost) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.SPELL);
		card.setCost(cost);
		return card;
	}

	private static BoardCreature creature(GamePlayerState owner, String name, int attack, int health, int cost, Set<Keyword> keywords) {
		Card card = creatureCard(name, cost, attack, health);
		BoardCreature creature = new BoardCreature(owner, card, attack, health, new HashSet<>(keywords), owner.getBattlefield().size());
		creature.setId(UUID.randomUUID());
		return creature;
	}
}
