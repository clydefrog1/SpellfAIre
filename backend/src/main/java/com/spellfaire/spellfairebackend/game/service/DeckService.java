package com.spellfaire.spellfairebackend.game.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.auth.repo.UserRepository;
import com.spellfaire.spellfairebackend.game.dto.CreateDeckRequest;
import com.spellfaire.spellfairebackend.game.dto.DeckCardRequest;
import com.spellfaire.spellfairebackend.game.dto.DeckCardResponse;
import com.spellfaire.spellfairebackend.game.dto.DeckResponse;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Deck;
import com.spellfaire.spellfairebackend.game.model.DeckCard;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;
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
	private final UserRepository userRepository;

	public DeckService(DeckRepository deckRepository, CardRepository cardRepository, UserRepository userRepository) {
		this.deckRepository = deckRepository;
		this.cardRepository = cardRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Create a new deck for a user.
	 * Validates deck composition according to game rules.
	 */
	@Transactional
	public DeckResponse createDeck(UUID userId, CreateDeckRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));

		// Validate deck composition
		List<ResolvedDeckCard> resolvedCards = resolveDeckCards(request);
		validateDeckComposition(request, resolvedCards);

		Deck deck = new Deck();
		deck.setUser(user);
		deck.setName(request.getName());
		deck.setFaction(request.getFaction());
		deck.setMagicSchool(request.getMagicSchool());

		Instant now = Instant.now();
		deck.setCreatedAt(now);
		deck.setUpdatedAt(now);

		// Create DeckCard entities
		for (ResolvedDeckCard rc : resolvedCards) {
			DeckCard deckCard = new DeckCard(deck, rc.card(), rc.quantity());
			deck.getDeckCards().add(deckCard);
		}

		deck = deckRepository.save(deck);
		return toResponse(deck);
	}

	/**
	 * Get all decks for a user.
	 */
	@Transactional(readOnly = true)
	public List<DeckResponse> getUserDecks(UUID userId) {
		return deckRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
			.map(this::toResponse)
			.toList();
	}

	/**
	 * Get a specific deck by ID.
	 */
	@Transactional(readOnly = true)
	public Optional<DeckResponse> getDeckById(UUID deckId) {
		return deckRepository.findById(deckId)
			.map(this::toResponse);
	}

	/**
	 * Update an existing deck.
	 */
	@Transactional
	public Optional<DeckResponse> updateDeck(UUID deckId, UUID userId, CreateDeckRequest request) {
		return deckRepository.findById(deckId)
			.filter(deck -> deck.getUser().getId().equals(userId))  // Ensure user owns the deck
			.map(deck -> {
				List<ResolvedDeckCard> resolvedCards = resolveDeckCards(request);
				validateDeckComposition(request, resolvedCards);

				deck.setName(request.getName());
				deck.setFaction(request.getFaction());
				deck.setMagicSchool(request.getMagicSchool());

				// Replace deck cards
				deck.getDeckCards().clear();
				for (ResolvedDeckCard rc : resolvedCards) {
					DeckCard deckCard = new DeckCard(deck, rc.card(), rc.quantity());
					deck.getDeckCards().add(deckCard);
				}
				deck.setUpdatedAt(Instant.now());

				return toResponse(deckRepository.save(deck));
			});
	}

	/**
	 * Delete a deck.
	 */
	@Transactional
	public boolean deleteDeck(UUID deckId, UUID userId) {
		return deckRepository.findById(deckId)
			.filter(deck -> deck.getUser().getId().equals(userId))
			.map(deck -> {
				deckRepository.delete(deck);
				return true;
			})
			.orElse(false);
	}

	/**
	 * Resolve card IDs from the request to Card entities.
	 */
	private List<ResolvedDeckCard> resolveDeckCards(CreateDeckRequest request) {
		List<ResolvedDeckCard> resolved = new ArrayList<>();
		for (DeckCardRequest cardRequest : request.getCards()) {
			Card card = cardRepository.findById(UUID.fromString(cardRequest.getCardId()))
				.orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardRequest.getCardId()));
			resolved.add(new ResolvedDeckCard(card, cardRequest.getQuantity()));
		}
		return resolved;
	}

	/**
	 * Validate deck composition according to game rules:
	 * - Exactly 24 cards total
	 * - Exactly 14 creatures from the chosen faction
	 * - Exactly 10 spells from the chosen magic school
	 * - Max 2 copies of any card
	 */
	private void validateDeckComposition(CreateDeckRequest request, List<ResolvedDeckCard> resolvedCards) {
		int totalCards = 0;
		int creatureCount = 0;
		int spellCount = 0;

		for (ResolvedDeckCard rc : resolvedCards) {
			Card card = rc.card();
			totalCards += rc.quantity();

			// Validate card belongs to chosen faction/school
			if (card.getCardType() == CardType.CREATURE) {
				if (card.getFaction() != request.getFaction()) {
					throw new IllegalArgumentException(
						"Creature " + card.getName() + " does not belong to faction " + request.getFaction()
					);
				}
				creatureCount += rc.quantity();
			} else if (card.getCardType() == CardType.SPELL) {
				if (card.getSchool() != request.getMagicSchool()) {
					throw new IllegalArgumentException(
						"Spell " + card.getName() + " does not belong to school " + request.getMagicSchool()
					);
				}
				spellCount += rc.quantity();
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
		response.setId(deck.getId().toString());
		response.setUserId(deck.getUser().getId().toString());
		response.setName(deck.getName());
		response.setFaction(deck.getFaction());
		response.setMagicSchool(deck.getMagicSchool());
		response.setCards(deck.getDeckCards().stream()
			.map(dc -> new DeckCardResponse(dc.getCard().getId().toString(), dc.getQuantity()))
			.toList());
		response.setCreatedAt(deck.getCreatedAt());
		response.setUpdatedAt(deck.getUpdatedAt());
		return response;
	}

	/**
	 * Build and persist an auto-generated deck for a user.
	 * Picks 2x each of the 7 faction creatures (14) and
	 * 2x each of the 5 cheapest spells from the school (10) = 24 cards.
	 */
	@Transactional
	public Deck buildAutoDeck(User user, Faction faction, MagicSchool magicSchool) {
		List<Card> creatures = cardRepository.findByCardTypeAndFaction(CardType.CREATURE, faction);
		List<Card> spells = cardRepository.findByCardTypeAndSchool(CardType.SPELL, magicSchool)
			.stream()
			.sorted(Comparator.comparingInt(Card::getCost))
			.limit(5)
			.toList();

		if (creatures.size() < 7) {
			throw new IllegalStateException("Not enough creatures for faction " + faction);
		}
		if (spells.size() < 5) {
			throw new IllegalStateException("Not enough spells for school " + magicSchool);
		}

		Deck deck = new Deck();
		deck.setUser(user);
		deck.setName("Auto " + faction.name() + " / " + magicSchool.name());
		deck.setFaction(faction);
		deck.setMagicSchool(magicSchool);

		Instant now = Instant.now();
		deck.setCreatedAt(now);
		deck.setUpdatedAt(now);

		for (Card creature : creatures) {
			deck.getDeckCards().add(new DeckCard(deck, creature, 2));
		}
		for (Card spell : spells) {
			deck.getDeckCards().add(new DeckCard(deck, spell, 2));
		}

		return deckRepository.save(deck);
	}

	private record ResolvedDeckCard(Card card, int quantity) {
	}
}
