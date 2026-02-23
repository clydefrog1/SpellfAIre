package com.spellfaire.spellfairebackend.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
	private CardRepository cardRepository;

	@BeforeEach
	void setUp() {
		cardRepository = Mockito.mock(CardRepository.class);
		resolver = new SpellEffectResolver(cardRepository);
	}

	@Test
	void emberBoltDamagesEnemyHero() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		Card emberBolt = fireSpell("Ember Bolt", 1);
		List<GameEvent> events = resolver.resolveSpell(emberBolt, caster, opponent, "ENEMY_HERO");

		assertEquals(23, opponent.getHeroHealth());
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.DAMAGE));
	}

	@Test
	void iceShardFreezesSurvivingCreature() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature target = creature(opponent, "Target", 2, 3, 2);
		opponent.getBattlefield().add(target);

		Card iceShard = frostSpell("Ice Shard", 1);
		List<GameEvent> events = resolver.resolveSpell(iceShard, caster, opponent, target.getId().toString());

		assertEquals(2, target.getHealth());
		assertTrue(target.getStatuses().contains(com.spellfaire.spellfairebackend.game.model.Status.FROZEN));
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.FREEZE));
	}

	@Test
	void shatterOnlyAffectsFrozenCreature() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature frozen = creature(opponent, "Frozen", 2, 4, 3);
		frozen.setStatuses(new HashSet<>(Set.of(com.spellfaire.spellfairebackend.game.model.Status.FROZEN)));
		BoardCreature nonFrozen = creature(opponent, "Not Frozen", 2, 4, 3);
		opponent.getBattlefield().add(frozen);
		opponent.getBattlefield().add(nonFrozen);

		Card shatter = frostSpell("Shatter", 4);
		resolver.resolveSpell(shatter, caster, opponent, nonFrozen.getId().toString());
		assertTrue(opponent.getBattlefield().contains(nonFrozen));

		resolver.resolveSpell(shatter, caster, opponent, frozen.getId().toString());
		assertFalse(opponent.getBattlefield().contains(frozen));
	}

	@Test
	void sproutlingSummonsTokenWhenSpaceAvailable() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		Card token = creatureCard("Sproutling", 1, 1, 1);
		when(cardRepository.findByName("Sproutling")).thenReturn(java.util.Optional.of(token));

		Card sproutling = natureSpell("Sproutling", 2);
		List<GameEvent> events = resolver.resolveSpell(sproutling, caster, opponent, null);

		assertEquals(1, caster.getBattlefield().size());
		assertEquals("Sproutling", caster.getBattlefield().getFirst().getCard().getName());
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.SUMMON));
	}

	@Test
	void renewalHealsAndDrawsCard() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");
		caster.setHeroHealth(20);
		caster.getZoneCards().add(new PlayerZoneCard(caster, creatureCard("Deck Creature", 2, 2, 2), CardZone.DECK, 0));

		Card renewal = natureSpell("Renewal", 5);
		List<GameEvent> events = resolver.resolveSpell(renewal, caster, opponent, null);

		assertEquals(25, caster.getHeroHealth());
		assertEquals(1, caster.getZoneCards().stream().filter(c -> c.getZone() == CardZone.HAND).count());
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.HEAL));
	}

	@Test
	void applyDamageToCreatureConsumesWardAndPreventsDamage() {
		GamePlayerState owner = playerState("owner");
		BoardCreature warded = creature(owner, "Warded", 2, 4, 2);
		warded.setKeywords(new HashSet<>(Set.of(com.spellfaire.spellfairebackend.game.model.Keyword.WARD)));

		List<GameEvent> events = new java.util.ArrayList<>();
		int damage = resolver.applyDamageToCreature(warded, 3, events, "Test Source");

		assertEquals(0, damage);
		assertEquals(4, warded.getHealth());
		assertFalse(warded.getKeywords().contains(com.spellfaire.spellfairebackend.game.model.Keyword.WARD));
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.BUFF));
	}

	@Test
	void drawCardAppliesFatigueWhenDeckEmpty() {
		GamePlayerState player = playerState("player");
		player.setHeroHealth(10);
		List<GameEvent> events = new java.util.ArrayList<>();

		resolver.drawCard(player, events);

		assertEquals(9, player.getHeroHealth());
		assertEquals(1, player.getFatigueCounter());
		assertTrue(events.stream().anyMatch(e -> e.getType() == GameEvent.EventType.FATIGUE));
	}

	@Test
	void drawCardBurnsWhenHandFull() {
		GamePlayerState player = playerState("player");
		for (int i = 0; i < 10; i++) {
			player.getZoneCards().add(new PlayerZoneCard(player, creatureCard("Hand " + i, 1, 1, 1), CardZone.HAND, i));
		}
		Card deckCard = creatureCard("Top Deck", 2, 2, 2);
		PlayerZoneCard topDeck = new PlayerZoneCard(player, deckCard, CardZone.DECK, 0);
		player.getZoneCards().add(topDeck);

		List<GameEvent> events = new java.util.ArrayList<>();
		resolver.drawCard(player, events);

		assertFalse(player.getZoneCards().contains(topDeck));
		assertEquals(10, player.getZoneCards().stream().filter(c -> c.getZone() == CardZone.HAND).count());
		assertTrue(events.stream().anyMatch(e -> e.getMessage() != null && e.getMessage().contains("Card burned")));
	}

	@Test
	void searingPingDamagesAllEnemyCreaturesAndKillsFragileOnes() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature survivor = creature(opponent, "Survivor", 2, 2, 2);
		BoardCreature doomed = creature(opponent, "Doomed", 1, 1, 1);
		opponent.getBattlefield().add(survivor);
		opponent.getBattlefield().add(doomed);

		Card searingPing = fireSpell("Searing Ping", 2);
		resolver.resolveSpell(searingPing, caster, opponent, null);

		assertEquals(1, survivor.getHealth());
		assertFalse(opponent.getBattlefield().contains(doomed));
	}

	@Test
	void combustDamagesCreatureAndEnemyHero() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature target = creature(opponent, "Target", 2, 3, 2);
		opponent.getBattlefield().add(target);

		Card combust = fireSpell("Combust", 4);
		resolver.resolveSpell(combust, caster, opponent, target.getId().toString());

		assertFalse(opponent.getBattlefield().contains(target));
		assertEquals(23, opponent.getHeroHealth());
	}

	@Test
	void infernoSweepHitsBothSidesAndKillsLowHealthCreatures() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature friendly = creature(caster, "Friendly", 2, 2, 2);
		BoardCreature enemy = creature(opponent, "Enemy", 2, 2, 2);
		caster.getBattlefield().add(friendly);
		opponent.getBattlefield().add(enemy);

		Card sweep = fireSpell("Inferno Sweep", 5);
		resolver.resolveSpell(sweep, caster, opponent, null);

		assertFalse(caster.getBattlefield().contains(friendly));
		assertFalse(opponent.getBattlefield().contains(enemy));
	}

	@Test
	void frostShieldBuffsFriendlyCreatureHealthAndMaxHealth() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature friendly = creature(caster, "Friendly", 2, 2, 2);
		caster.getBattlefield().add(friendly);

		Card frostShield = frostSpell("Frost Shield", 2);
		resolver.resolveSpell(frostShield, caster, opponent, friendly.getId().toString());

		assertEquals(5, friendly.getHealth());
		assertEquals(5, friendly.getMaxHealth());
	}

	@Test
	void glacialBindingFreezesThenDamagesEnemyCreature() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature enemy = creature(opponent, "Enemy", 2, 4, 2);
		opponent.getBattlefield().add(enemy);

		Card glacialBinding = frostSpell("Glacial Binding", 5);
		resolver.resolveSpell(glacialBinding, caster, opponent, enemy.getId().toString());

		assertEquals(1, enemy.getHealth());
		assertTrue(enemy.getStatuses().contains(com.spellfaire.spellfairebackend.game.model.Status.FROZEN));
	}

	@Test
	void mendHealsHeroWithCap() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");
		caster.setHeroHealth(24);

		Card mend = natureSpell("Mend", 1);
		resolver.resolveSpell(mend, caster, opponent, null);

		assertEquals(25, caster.getHeroHealth());
	}

	@Test
	void vineWhipFreezesIfTargetSurvives() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature enemy = creature(opponent, "Enemy", 2, 4, 2);
		opponent.getBattlefield().add(enemy);

		Card vineWhip = natureSpell("Vine Whip", 2);
		resolver.resolveSpell(vineWhip, caster, opponent, enemy.getId().toString());

		assertEquals(2, enemy.getHealth());
		assertTrue(enemy.getStatuses().contains(com.spellfaire.spellfairebackend.game.model.Status.FROZEN));
	}

	@Test
	void growthBuffsAttackAndHealth() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		BoardCreature friendly = creature(caster, "Friendly", 2, 2, 2);
		caster.getBattlefield().add(friendly);

		Card growth = natureSpell("Growth", 3);
		resolver.resolveSpell(growth, caster, opponent, friendly.getId().toString());

		assertEquals(4, friendly.getAttack());
		assertEquals(4, friendly.getHealth());
		assertEquals(4, friendly.getMaxHealth());
	}

	@Test
	void brambleWallDoesNotSummonWhenBattlefieldFull() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		for (int i = 0; i < 6; i++) {
			caster.getBattlefield().add(creature(caster, "Filled" + i, 1, 1, 1));
		}

		Card brambleToken = creatureCard("Bramble Wall", 4, 0, 6);
		when(cardRepository.findByName("Bramble Wall")).thenReturn(java.util.Optional.of(brambleToken));

		Card brambleWall = natureSpell("Bramble Wall", 4);
		resolver.resolveSpell(brambleWall, caster, opponent, null);

		assertEquals(6, caster.getBattlefield().size());
	}

	@Test
	void siphonLifeDamagesEnemyHeroAndHealsCaster() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");
		caster.setHeroHealth(20);

		Card siphonLife = shadowSpell("Siphon Life", 3);
		resolver.resolveSpell(siphonLife, caster, opponent, null);

		assertEquals(23, caster.getHeroHealth());
		assertEquals(22, opponent.getHeroHealth());
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

	@Test
	void unknownSpellReturnsSpellResolvedEvent() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		Card unknown = shadowSpell("Mystery Blast", 2);
		List<GameEvent> events = resolver.resolveSpell(unknown, caster, opponent, null);

		assertEquals(1, events.size());
		assertEquals(GameEvent.EventType.SPELL_RESOLVED, events.getFirst().getType());
		assertTrue(events.getFirst().getMessage().contains("Unknown spell"));
	}

	@Test
	void darkTouchCanDamageFriendlyHero() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");
		caster.setHeroHealth(20);

		Card darkTouch = shadowSpell("Dark Touch", 1);
		resolver.resolveSpell(darkTouch, caster, opponent, "FRIENDLY_HERO");

		assertEquals(20, caster.getHeroHealth());
	}

	@Test
	void deepWinterDrawsTwoAndFreezesSingleEnemyCreature() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		caster.getZoneCards().add(new PlayerZoneCard(caster, creatureCard("Deck One", 1, 1, 1), CardZone.DECK, 0));
		caster.getZoneCards().add(new PlayerZoneCard(caster, creatureCard("Deck Two", 2, 2, 2), CardZone.DECK, 1));

		BoardCreature enemy = creature(opponent, "Enemy", 2, 3, 2);
		opponent.getBattlefield().add(enemy);

		Card deepWinter = frostSpell("Deep Winter", 6);
		List<GameEvent> events = resolver.resolveSpell(deepWinter, caster, opponent, null);

		assertEquals(2, caster.getZoneCards().stream().filter(c -> c.getZone() == CardZone.HAND).count());
		assertTrue(enemy.getStatuses().contains(com.spellfaire.spellfairebackend.game.model.Status.FROZEN));
		assertTrue(events.stream().anyMatch(event -> event.getType() == GameEvent.EventType.FREEZE));
	}

	@Test
	void sproutlingThrowsWhenTokenMissing() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		when(cardRepository.findByName("Sproutling")).thenReturn(java.util.Optional.empty());

		Card sproutling = natureSpell("Sproutling", 2);
		assertThrows(IllegalStateException.class, () -> resolver.resolveSpell(sproutling, caster, opponent, null));
	}

	@Test
	void brambleWallThrowsWhenTokenMissing() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		when(cardRepository.findByName("Bramble Wall")).thenReturn(java.util.Optional.empty());

		Card brambleWall = natureSpell("Bramble Wall", 4);
		assertThrows(IllegalStateException.class, () -> resolver.resolveSpell(brambleWall, caster, opponent, null));
	}

	@Test
	void grimBargainNoOpsWhenTargetMissing() {
		GamePlayerState caster = playerState("caster");
		GamePlayerState opponent = playerState("opponent");

		Card grimBargain = shadowSpell("Grim Bargain", 4);
		List<GameEvent> events = resolver.resolveSpell(grimBargain, caster, opponent, UUID.randomUUID().toString());

		assertTrue(events.isEmpty());
		assertEquals(0, caster.getZoneCards().stream().filter(card -> card.getZone() == CardZone.HAND).count());
	}

	@Test
	void drawCardAppliesIncreasingFatigueOnRepeatedEmptyDraws() {
		GamePlayerState player = playerState("player");
		player.setHeroHealth(10);
		List<GameEvent> events = new java.util.ArrayList<>();

		resolver.drawCard(player, events);
		resolver.drawCard(player, events);

		assertEquals(7, player.getHeroHealth());
		assertEquals(2, player.getFatigueCounter());
		assertEquals(2, events.stream().filter(event -> event.getType() == GameEvent.EventType.FATIGUE).count());
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

	private static Card fireSpell(String name, int cost) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.SPELL);
		card.setCost(cost);
		card.setSchool(MagicSchool.FIRE);
		return card;
	}

	private static Card frostSpell(String name, int cost) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.SPELL);
		card.setCost(cost);
		card.setSchool(MagicSchool.FROST);
		return card;
	}

	private static Card natureSpell(String name, int cost) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.SPELL);
		card.setCost(cost);
		card.setSchool(MagicSchool.NATURE);
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
		assertNotNull(creature.getId());
		return creature;
	}

}