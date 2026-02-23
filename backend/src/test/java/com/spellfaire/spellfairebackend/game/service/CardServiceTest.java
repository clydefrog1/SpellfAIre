package com.spellfaire.spellfairebackend.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

	@Mock
	private CardRepository cardRepository;

	private CardService cardService;

	@BeforeEach
	void setUp() {
		cardService = new CardService(cardRepository);
	}

	@Test
	void getAllCardsMapsRepositoryResult() {
		Card card = creatureCard("Town Guard", Faction.KINGDOM, 1, 1, 2);
		when(cardRepository.findAll()).thenReturn(List.of(card));

		var result = cardService.getAllCards();

		assertEquals(1, result.size());
		assertEquals("Town Guard", result.getFirst().getName());
	}

	@Test
	void getCardByIdReturnsMappedOptionalWhenFound() {
		UUID id = UUID.randomUUID();
		Card card = spellCard("Final Spark", MagicSchool.FIRE, 6);
		card.setId(id);
		when(cardRepository.findById(id)).thenReturn(Optional.of(card));

		var result = cardService.getCardById(id);

		assertTrue(result.isPresent());
		assertEquals("Final Spark", result.get().getName());
	}

	@Test
	void getCreaturesByFactionUsesCorrectRepositoryFilter() {
		Card creature = creatureCard("Shield Marshal", Faction.KINGDOM, 4, 3, 5);
		when(cardRepository.findByCardTypeAndFaction(CardType.CREATURE, Faction.KINGDOM)).thenReturn(List.of(creature));

		var result = cardService.getCreaturesByFaction(Faction.KINGDOM);

		assertEquals(1, result.size());
		assertEquals(CardType.CREATURE, result.getFirst().getCardType());
		assertEquals(Faction.KINGDOM, result.getFirst().getFaction());
	}

	@Test
	void getSpellsBySchoolUsesCorrectRepositoryFilter() {
		Card spell = spellCard("Mend", MagicSchool.NATURE, 1);
		when(cardRepository.findByCardTypeAndSchool(CardType.SPELL, MagicSchool.NATURE)).thenReturn(List.of(spell));

		var result = cardService.getSpellsBySchool(MagicSchool.NATURE);

		assertEquals(1, result.size());
		assertEquals(CardType.SPELL, result.getFirst().getCardType());
		assertEquals(MagicSchool.NATURE, result.getFirst().getSchool());
	}

	private static Card creatureCard(String name, Faction faction, int cost, int attack, int health) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.CREATURE);
		card.setFaction(faction);
		card.setCost(cost);
		card.setAttack(attack);
		card.setHealth(health);
		return card;
	}

	private static Card spellCard(String name, MagicSchool school, int cost) {
		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName(name);
		card.setCardType(CardType.SPELL);
		card.setSchool(school);
		card.setCost(cost);
		return card;
	}
}
