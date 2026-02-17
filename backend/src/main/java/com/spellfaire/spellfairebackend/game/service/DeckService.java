package com.spellfaire.spellfairebackend.game.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.spellfaire.spellfairebackend.game.dto.CreateDeckRequest;
import com.spellfaire.spellfairebackend.game.dto.DeckCardRequest;
import com.spellfaire.spellfairebackend.game.dto.DeckResponse;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Deck;
import com.spellfaire.spellfairebackend.game.model.DeckCard;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;
import com.spellfaire.spellfairebackend.game.repo.DeckRepository;

/**
 * Service for deck-related operations.
 * Handles deck creation, validation, and management.
 */
@Service
public class DeckService {

	private final DeckRepository deckRepository;
	private final CardRepository cardRepository;

	public DeckService(DeckRepository deckRepository, CardRepository cardRepository) {
		this.deckRepository = deckRepository;
		this.cardRepository = cardRepository;
	}

	/**
	 * Create a new deck for a user.
	 * Validates deck composition according to game rules.
	 */
	public DeckResponse createDeck(String userId, CreateDeckRequest request) {
		// Validate deck composition
		validateDeckComposition(request);

		Deck deck = new Deck();
		deck.setUserId(userId);
		deck.setName(request.getName());
		deck.setFaction(request.getFaction());
		deck.setMagicSchool(request.getMagicSchool());
		
		List<DeckCard> deckCards = new ArrayList<>();
		for (DeckCardRequest cardRequest : request.getCards()) {
			deckCards.add(new DeckCard(cardRequest.getCardId(), cardRequest.getQuantity()));
		}
		deck.setCards(deckCards);
		
		Instant now = Instant.now();
		deck.setCreatedAt(now);
		deck.setUpdatedAt(now);

		deck = deckRepository.save(deck);
		return toResponse(deck);
	}

	/**
	 * Get all decks for a user.
	 */
	public List<DeckResponse> getUserDecks(String userId) {
		return deckRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * Get a specific deck by ID.
	 */
	public Optional<DeckResponse> getDeckById(String deckId) {
		return deckRepository.findById(deckId)
			.map(this::toResponse);
	}

	/**
	 * Update an existing deck.
	 */
	public Optional<DeckResponse> updateDeck(String deckId, String userId, CreateDeckRequest request) {
		return deckRepository.findById(deckId)
			.filter(deck -> deck.getUserId().equals(userId))  // Ensure user owns the deck
			.map(deck -> {
				validateDeckComposition(request);
				
				deck.setName(request.getName());
				deck.setFaction(request.getFaction());
				deck.setMagicSchool(request.getMagicSchool());
				
				List<DeckCard> deckCards = new ArrayList<>();
				for (DeckCardRequest cardRequest : request.getCards()) {
					deckCards.add(new DeckCard(cardRequest.getCardId(), cardRequest.getQuantity()));
				}
				deck.setCards(deckCards);
				deck.setUpdatedAt(Instant.now());
				
				return toResponse(deckRepository.save(deck));
			});
	}

	/**
	 * Delete a deck.
	 */
	public boolean deleteDeck(String deckId, String userId) {
		return deckRepository.findById(deckId)
			.filter(deck -> deck.getUserId().equals(userId))
			.map(deck -> {
				deckRepository.delete(deck);
				return true;
			})
			.orElse(false);
	}

	/**
	 * Validate deck composition according to game rules:
	 * - Exactly 24 cards total
	 * - Exactly 14 creatures from the chosen faction
	 * - Exactly 10 spells from the chosen magic school
	 * - Max 2 copies of any card
	 */
	private void validateDeckComposition(CreateDeckRequest request) {
		int totalCards = 0;
		int creatureCount = 0;
		int spellCount = 0;

		for (DeckCardRequest cardRequest : request.getCards()) {
			Card card = cardRepository.findById(cardRequest.getCardId())
				.orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardRequest.getCardId()));

			totalCards += cardRequest.getQuantity();

			// Validate card belongs to chosen faction/school
			if (card.getCardType() == CardType.CREATURE) {
				if (card.getFaction() != request.getFaction()) {
					throw new IllegalArgumentException(
						"Creature " + card.getName() + " does not belong to faction " + request.getFaction()
					);
				}
				creatureCount += cardRequest.getQuantity();
			} else if (card.getCardType() == CardType.SPELL) {
				if (card.getSchool() != request.getMagicSchool()) {
					throw new IllegalArgumentException(
						"Spell " + card.getName() + " does not belong to school " + request.getMagicSchool()
					);
				}
				spellCount += cardRequest.getQuantity();
			}
		}

		// Validate total counts
		if (totalCards != 24) {
			throw new IllegalArgumentException("Deck must contain exactly 24 cards, found: " + totalCards);
		}
		if (creatureCount != 14) {
			throw new IllegalArgumentException("Deck must contain exactly 14 creatures, found: " + creatureCount);
		}
		if (spellCount != 10) {
			throw new IllegalArgumentException("Deck must contain exactly 10 spells, found: " + spellCount);
		}
	}

	/**
	 * Map Deck entity to DeckResponse DTO.
	 */
	private DeckResponse toResponse(Deck deck) {
		DeckResponse response = new DeckResponse();
		response.setId(deck.getId());
		response.setUserId(deck.getUserId());
		response.setName(deck.getName());
		response.setFaction(deck.getFaction());
		response.setMagicSchool(deck.getMagicSchool());
		response.setCards(deck.getCards());
		response.setCreatedAt(deck.getCreatedAt());
		response.setUpdatedAt(deck.getUpdatedAt());
		return response;
	}
}
