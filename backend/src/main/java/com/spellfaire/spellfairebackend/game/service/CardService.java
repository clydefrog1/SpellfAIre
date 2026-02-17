package com.spellfaire.spellfairebackend.game.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.spellfaire.spellfairebackend.game.dto.CardResponse;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;

/**
 * Service for card-related operations.
 * Cards are static game data, so this service is primarily read-only.
 */
@Service
public class CardService {

	private final CardRepository cardRepository;

	public CardService(CardRepository cardRepository) {
		this.cardRepository = cardRepository;
	}

	/**
	 * Get all cards.
	 */
	public List<CardResponse> getAllCards() {
		return cardRepository.findAll().stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * Get a card by ID.
	 */
	public Optional<CardResponse> getCardById(UUID id) {
		return cardRepository.findById(id)
			.map(this::toResponse);
	}

	/**
	 * Get all creatures.
	 */
	public List<CardResponse> getCreatures() {
		return cardRepository.findByCardType(CardType.CREATURE).stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * Get all spells.
	 */
	public List<CardResponse> getSpells() {
		return cardRepository.findByCardType(CardType.SPELL).stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * Get creatures by faction.
	 */
	public List<CardResponse> getCreaturesByFaction(Faction faction) {
		return cardRepository.findByCardTypeAndFaction(CardType.CREATURE, faction).stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * Get spells by magic school.
	 */
	public List<CardResponse> getSpellsBySchool(MagicSchool school) {
		return cardRepository.findByCardTypeAndSchool(CardType.SPELL, school).stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * Map Card entity to CardResponse DTO.
	 */
	private CardResponse toResponse(Card card) {
		CardResponse response = new CardResponse();
		response.setId(card.getId().toString());
		response.setName(card.getName());
		response.setCardType(card.getCardType());
		response.setCost(card.getCost());
		response.setAttack(card.getAttack());
		response.setHealth(card.getHealth());
		response.setFaction(card.getFaction());
		response.setKeywords(card.getKeywords());
		response.setSchool(card.getSchool());
		response.setRulesText(card.getRulesText());
		return response;
	}
}
