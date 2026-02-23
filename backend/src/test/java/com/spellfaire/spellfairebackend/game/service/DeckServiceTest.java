package com.spellfaire.spellfairebackend.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.UserRepository;
import com.spellfaire.spellfairebackend.game.dto.CreateDeckRequest;
import com.spellfaire.spellfairebackend.game.dto.DeckCardRequest;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Deck;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;
import com.spellfaire.spellfairebackend.game.repo.DeckRepository;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

	@Mock
	private DeckRepository deckRepository;

	@Mock
	private CardRepository cardRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private DeckService deckService;

	private User user;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("deck.tester@example.com");
		user.setUsername("DeckTester");
		user.setPasswordHash("hashed");
		user.setCreatedAt(Instant.now());
	}

	@Test
	void createDeckRejectsInvalidTotalCards() {
		Card creature = creatureCard("Kingdom Squire", Faction.KINGDOM);
		Card spell = spellCard("Fire Bolt", MagicSchool.FIRE, 1);

		CreateDeckRequest request = new CreateDeckRequest();
		request.setName("Too Small");
		request.setFaction(Faction.KINGDOM);
		request.setMagicSchool(MagicSchool.FIRE);
		request.setCards(List.of(
				deckCardRequest(creature.getId().toString(), 1),
				deckCardRequest(spell.getId().toString(), 1)));

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(cardRepository.findById(creature.getId())).thenReturn(Optional.of(creature));
		when(cardRepository.findById(spell.getId())).thenReturn(Optional.of(spell));

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> deckService.createDeck(user.getId(), request));

		assertTrue(exception.getMessage().contains("exactly 24 cards"));
	}

	@Test
	void createDeckRejectsSpellFromWrongSchool() {
		Card creature = creatureCard("Kingdom Guard", Faction.KINGDOM);
		Card wrongSchoolSpell = spellCard("Shadow Drain", MagicSchool.SHADOW, 2);

		CreateDeckRequest request = new CreateDeckRequest();
		request.setName("Wrong School");
		request.setFaction(Faction.KINGDOM);
		request.setMagicSchool(MagicSchool.FIRE);
		request.setCards(List.of(
				deckCardRequest(creature.getId().toString(), 14),
				deckCardRequest(wrongSchoolSpell.getId().toString(), 10)));

		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
		when(cardRepository.findById(creature.getId())).thenReturn(Optional.of(creature));
		when(cardRepository.findById(wrongSchoolSpell.getId())).thenReturn(Optional.of(wrongSchoolSpell));

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> deckService.createDeck(user.getId(), request));

		assertTrue(exception.getMessage().contains("does not belong to school FIRE"));
	}

	@Test
	void buildAutoDeckUsesSevenCreaturesAndCheapestFiveSpells() {
		List<Card> creatures = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			creatures.add(creatureCard("Kingdom Creature " + i, Faction.KINGDOM));
		}

		List<Card> spells = List.of(
				spellCard("Spell 6", MagicSchool.FIRE, 6),
				spellCard("Spell 1", MagicSchool.FIRE, 1),
				spellCard("Spell 4", MagicSchool.FIRE, 4),
				spellCard("Spell 2", MagicSchool.FIRE, 2),
				spellCard("Spell 5", MagicSchool.FIRE, 5),
				spellCard("Spell 3", MagicSchool.FIRE, 3));

		when(cardRepository.findByCardTypeAndFaction(CardType.CREATURE, Faction.KINGDOM)).thenReturn(creatures);
		when(cardRepository.findByCardTypeAndSchool(CardType.SPELL, MagicSchool.FIRE)).thenReturn(spells);
		when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Deck deck = deckService.buildAutoDeck(user, Faction.KINGDOM, MagicSchool.FIRE);

		assertEquals(12, deck.getDeckCards().size());
		assertEquals(7, deck.getDeckCards().stream()
				.filter(dc -> dc.getCard().getCardType() == CardType.CREATURE)
				.count());
		assertEquals(5, deck.getDeckCards().stream()
				.filter(dc -> dc.getCard().getCardType() == CardType.SPELL)
				.count());
		assertTrue(deck.getDeckCards().stream().allMatch(dc -> dc.getQuantity() == 2));
		assertTrue(deck.getDeckCards().stream()
				.filter(dc -> dc.getCard().getCardType() == CardType.SPELL)
				.allMatch(dc -> dc.getCard().getCost() <= 5));
	}

	private static DeckCardRequest deckCardRequest(String cardId, int quantity) {
		DeckCardRequest request = new DeckCardRequest();
		request.setCardId(cardId);
		request.setQuantity(quantity);
		return request;
	}

	private static Card creatureCard(String name, Faction faction) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.CREATURE);
		card.setCost(2);
		card.setAttack(2);
		card.setHealth(2);
		card.setFaction(faction);
		return card;
	}

	private static Card spellCard(String name, MagicSchool school, int cost) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.SPELL);
		card.setCost(cost);
		card.setSchool(school);
		return card;
	}
}
