package com.spellfaire.spellfairebackend.game.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spellfaire.spellfairebackend.game.dto.BoardCreatureResponse;
import com.spellfaire.spellfairebackend.game.dto.CreateGameRequest;
import com.spellfaire.spellfairebackend.game.dto.GamePlayerStateResponse;
import com.spellfaire.spellfairebackend.game.dto.GameResponse;
import com.spellfaire.spellfairebackend.game.model.BoardCreature;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardZone;
import com.spellfaire.spellfairebackend.game.model.Deck;
import com.spellfaire.spellfairebackend.game.model.DeckCard;
import com.spellfaire.spellfairebackend.game.model.Game;
import com.spellfaire.spellfairebackend.game.model.GamePhase;
import com.spellfaire.spellfairebackend.game.model.GamePlayerState;
import com.spellfaire.spellfairebackend.game.model.GameStatus;
import com.spellfaire.spellfairebackend.game.model.PlayerZoneCard;
import com.spellfaire.spellfairebackend.game.repo.DeckRepository;
import com.spellfaire.spellfairebackend.game.repo.GameRepository;

/**
 * Service for game-related operations.
 * Handles game creation, setup, and state management.
 */
@Service
public class GameService {

	private final GameRepository gameRepository;
	private final DeckRepository deckRepository;
	private final Random random;

	public GameService(GameRepository gameRepository, DeckRepository deckRepository) {
		this.gameRepository = gameRepository;
		this.deckRepository = deckRepository;
		this.random = new Random();
	}

	/**
	 * Create a new game.
	 * Sets up initial game state including shuffled decks and initial hands.
	 */
	@Transactional
	public GameResponse createGame(String player1Id, CreateGameRequest request) {
		// Load player 1 deck
		Deck player1Deck = deckRepository.findById(UUID.fromString(request.getPlayer1DeckId()))
			.orElseThrow(() -> new IllegalArgumentException("Player 1 deck not found"));

		if (!player1Deck.getUser().getId().toString().equals(player1Id)) {
			throw new IllegalArgumentException("Player 1 does not own the selected deck");
		}

		Game game = new Game();
		game.setPlayer1Id(player1Id);

		// Handle AI or player 2
		if (request.getPlayer2Id() == null || "AI".equalsIgnoreCase(request.getPlayer2Id())) {
			game.setPlayer2Id("AI");
			// For AI games, we'll need to generate or select an AI deck
			// For now, throw an exception to indicate AI implementation is needed
			throw new UnsupportedOperationException("AI games not yet implemented");
		} else {
			game.setPlayer2Id(request.getPlayer2Id());

			if (request.getPlayer2DeckId() == null) {
				throw new IllegalArgumentException("Player 2 deck ID is required for player vs player games");
			}

			Deck player2Deck = deckRepository.findById(UUID.fromString(request.getPlayer2DeckId()))
				.orElseThrow(() -> new IllegalArgumentException("Player 2 deck not found"));

			if (!player2Deck.getUser().getId().toString().equals(request.getPlayer2Id())) {
				throw new IllegalArgumentException("Player 2 does not own the selected deck");
			}

			// Initialize player 2 state
			initializePlayerState(game.getPlayer2State(), request.getPlayer2Id(), player2Deck, false);
		}

		// Randomly determine who goes first
		boolean player1First = random.nextBoolean();
		game.setCurrentPlayerId(player1First ? player1Id : game.getPlayer2Id());

		// Initialize player 1 state
		initializePlayerState(game.getPlayer1State(), player1Id, player1Deck, player1First);

		game.setGameStatus(GameStatus.SETUP);
		game.setCurrentPhase(GamePhase.MAIN);
		game.setTurnNumber(0);

		Instant now = Instant.now();
		game.setCreatedAt(now);
		game.setUpdatedAt(now);

		game = gameRepository.save(game);
		return toGameResponse(game);
	}

	/**
	 * Get all games for a player.
	 */
	@Transactional(readOnly = true)
	public List<GameResponse> getPlayerGames(String playerId) {
		return gameRepository.findByPlayer1IdOrPlayer2IdOrderByUpdatedAtDesc(playerId, playerId).stream()
			.map(this::toGameResponse)
			.toList();
	}

	/**
	 * Get active games for a player.
	 */
	@Transactional(readOnly = true)
	public List<GameResponse> getActiveGames(String playerId) {
		List<GameStatus> activeStatuses = List.of(GameStatus.SETUP, GameStatus.IN_PROGRESS);
		return gameRepository.findByPlayerIdAndGameStatusIn(playerId, activeStatuses).stream()
			.map(this::toGameResponse)
			.toList();
	}

	/**
	 * Get a specific game by ID.
	 */
	@Transactional(readOnly = true)
	public Optional<GameResponse> getGameById(UUID gameId) {
		return gameRepository.findById(gameId)
			.map(this::toGameResponse);
	}

	/**
	 * Initialize a player's game state with shuffled deck and initial hand.
	 */
	private void initializePlayerState(GamePlayerState state, String userId, Deck deck, boolean goesFirst) {
		state.setUserId(userId);
		state.setDeckId(deck.getId().toString());
		state.setHeroHealth(25);
		state.setMaxMana(1);
		state.setCurrentMana(1);
		state.setFatigueCounter(0);

		// Build and shuffle the deck
		List<Card> deckCards = buildDeckList(deck);
		Collections.shuffle(deckCards, random);

		// Draw initial hand (3 for first player, 4 for second)
		int initialHandSize = goesFirst ? 3 : 4;

		// Add cards to hand zone
		int position = 0;
		for (int i = 0; i < initialHandSize && i < deckCards.size(); i++) {
			PlayerZoneCard zoneCard = new PlayerZoneCard(state, deckCards.get(i), CardZone.HAND, position++);
			state.getZoneCards().add(zoneCard);
		}

		// Remaining cards go to deck zone
		position = 0;
		for (int i = initialHandSize; i < deckCards.size(); i++) {
			PlayerZoneCard zoneCard = new PlayerZoneCard(state, deckCards.get(i), CardZone.DECK, position++);
			state.getZoneCards().add(zoneCard);
		}

		state.setBattlefield(new ArrayList<>());
	}

	/**
	 * Build a list of Card entities from a deck, expanding quantities.
	 */
	private List<Card> buildDeckList(Deck deck) {
		List<Card> cardList = new ArrayList<>();
		for (DeckCard deckCard : deck.getDeckCards()) {
			for (int i = 0; i < deckCard.getQuantity(); i++) {
				cardList.add(deckCard.getCard());
			}
		}
		return cardList;
	}

	/**
	 * Map Game entity to GameResponse DTO (package-visible for GameplayService).
	 */
	public GameResponse toGameResponse(Game game) {
		GameResponse response = new GameResponse();
		response.setId(game.getId().toString());
		response.setPlayer1Id(game.getPlayer1Id());
		response.setPlayer2Id(game.getPlayer2Id());
		response.setCurrentPlayerId(game.getCurrentPlayerId());
		response.setGameStatus(game.getGameStatus());
		response.setCurrentPhase(game.getCurrentPhase());
		response.setWinnerId(game.getWinnerId());
		response.setTurnNumber(game.getTurnNumber());
		response.setPlayer1State(toPlayerStateResponse(game.getPlayer1State()));
		response.setPlayer2State(toPlayerStateResponse(game.getPlayer2State()));
		response.setCreatedAt(game.getCreatedAt());
		response.setUpdatedAt(game.getUpdatedAt());
		return response;
	}

	/**
	 * Map GamePlayerState entity to GamePlayerStateResponse DTO.
	 */
	private GamePlayerStateResponse toPlayerStateResponse(GamePlayerState state) {
		if (state == null) {
			return null;
		}

		GamePlayerStateResponse response = new GamePlayerStateResponse();
		response.setUserId(state.getUserId());
		response.setDeckId(state.getDeckId());
		response.setHeroHealth(state.getHeroHealth());
		response.setMaxMana(state.getMaxMana());
		response.setCurrentMana(state.getCurrentMana());
		response.setFatigueCounter(state.getFatigueCounter());

		// Separate zone cards by zone type
		response.setDeck(state.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.DECK)
			.map(c -> c.getCard().getId().toString())
			.collect(Collectors.toList()));

		response.setHand(state.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.HAND)
			.map(c -> c.getCard().getId().toString())
			.collect(Collectors.toList()));

		response.setDiscardPile(state.getZoneCards().stream()
			.filter(c -> c.getZone() == CardZone.DISCARD)
			.map(c -> c.getCard().getId().toString())
			.collect(Collectors.toList()));

		// Map battlefield creatures
		response.setBattlefield(state.getBattlefield().stream()
			.map(this::toBoardCreatureResponse)
			.collect(Collectors.toList()));

		return response;
	}

	/**
	 * Map BoardCreature entity to BoardCreatureResponse DTO.
	 */
	private BoardCreatureResponse toBoardCreatureResponse(BoardCreature creature) {
		BoardCreatureResponse response = new BoardCreatureResponse();
		response.setInstanceId(creature.getId().toString());
		response.setCardId(creature.getCard().getId().toString());
		response.setAttack(creature.getAttack());
		response.setHealth(creature.getHealth());
		response.setMaxHealth(creature.getMaxHealth());
		response.setCanAttack(creature.isCanAttack());
		response.setHasAttackedThisTurn(creature.isHasAttackedThisTurn());
		response.setKeywords(creature.getKeywords());
		response.setStatuses(creature.getStatuses());
		response.setPosition(creature.getPosition());
		return response;
	}
}
