package com.spellfaire.spellfairebackend.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spellfaire.spellfairebackend.auth.model.User;
import com.spellfaire.spellfairebackend.game.dto.CreateGameRequest;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Deck;
import com.spellfaire.spellfairebackend.game.model.DeckCard;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.Game;
import com.spellfaire.spellfairebackend.game.model.GamePhase;
import com.spellfaire.spellfairebackend.game.model.GameStatus;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;
import com.spellfaire.spellfairebackend.game.repo.DeckRepository;
import com.spellfaire.spellfairebackend.game.repo.GameRepository;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

	@Mock
	private GameRepository gameRepository;

	@Mock
	private DeckRepository deckRepository;

	private GameService gameService;

	@BeforeEach
	void setUp() {
		gameService = new GameService(gameRepository, deckRepository);
	}

	@Test
	void createGameRejectsDeckNotOwnedByPlayer() {
		String playerId = UUID.randomUUID().toString();
		String otherUserId = UUID.randomUUID().toString();
		UUID deckId = UUID.randomUUID();

		Deck playerDeck = deckForUser(otherUserId, deckId, Faction.KINGDOM, MagicSchool.FIRE);
		when(deckRepository.findById(deckId)).thenReturn(Optional.of(playerDeck));

		CreateGameRequest request = new CreateGameRequest();
		request.setPlayer1DeckId(deckId.toString());
		request.setPlayer2Id("AI");

		assertThrows(IllegalArgumentException.class, () -> gameService.createGame(playerId, request));
	}

	@Test
	void createGameAiPathThrowsUnsupportedOperation() {
		String playerId = UUID.randomUUID().toString();
		UUID deckId = UUID.randomUUID();

		Deck playerDeck = deckForUser(playerId, deckId, Faction.KINGDOM, MagicSchool.FIRE);
		when(deckRepository.findById(deckId)).thenReturn(Optional.of(playerDeck));

		CreateGameRequest request = new CreateGameRequest();
		request.setPlayer1DeckId(deckId.toString());
		request.setPlayer2Id("AI");

		assertThrows(UnsupportedOperationException.class, () -> gameService.createGame(playerId, request));
	}

	@Test
	void createGamePvpRequiresPlayer2DeckId() {
		String player1Id = UUID.randomUUID().toString();
		String player2Id = UUID.randomUUID().toString();
		UUID player1DeckId = UUID.randomUUID();

		Deck player1Deck = deckForUser(player1Id, player1DeckId, Faction.KINGDOM, MagicSchool.FIRE);
		when(deckRepository.findById(player1DeckId)).thenReturn(Optional.of(player1Deck));

		CreateGameRequest request = new CreateGameRequest();
		request.setPlayer1DeckId(player1DeckId.toString());
		request.setPlayer2Id(player2Id);
		request.setPlayer2DeckId(null);

		assertThrows(IllegalArgumentException.class, () -> gameService.createGame(player1Id, request));
	}

	@Test
	void createGamePvpReturnsInitializedSetupGame() {
		String player1Id = UUID.randomUUID().toString();
		String player2Id = UUID.randomUUID().toString();
		UUID player1DeckId = UUID.randomUUID();
		UUID player2DeckId = UUID.randomUUID();

		Deck player1Deck = deckForUser(player1Id, player1DeckId, Faction.KINGDOM, MagicSchool.FIRE);
		Deck player2Deck = deckForUser(player2Id, player2DeckId, Faction.WILDCLAN, MagicSchool.FROST);

		when(deckRepository.findById(player1DeckId)).thenReturn(Optional.of(player1Deck));
		when(deckRepository.findById(player2DeckId)).thenReturn(Optional.of(player2Deck));
		when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
			Game game = invocation.getArgument(0);
			if (game.getId() == null) {
				game.setId(UUID.randomUUID());
			}
			return game;
		});

		CreateGameRequest request = new CreateGameRequest();
		request.setPlayer1DeckId(player1DeckId.toString());
		request.setPlayer2Id(player2Id);
		request.setPlayer2DeckId(player2DeckId.toString());

		var response = gameService.createGame(player1Id, request);

		assertNotNull(response.getId());
		assertEquals(player1Id, response.getPlayer1Id());
		assertEquals(player2Id, response.getPlayer2Id());
		assertEquals(GameStatus.SETUP, response.getGameStatus());
		assertEquals(GamePhase.MAIN, response.getCurrentPhase());
		assertNotNull(response.getPlayer1State());
		assertNotNull(response.getPlayer2State());
	}

	@Test
	void createGameRejectsPlayer2DeckOwnedByDifferentUser() {
		String player1Id = UUID.randomUUID().toString();
		String player2Id = UUID.randomUUID().toString();
		String otherUserId = UUID.randomUUID().toString();
		UUID player1DeckId = UUID.randomUUID();
		UUID player2DeckId = UUID.randomUUID();

		Deck player1Deck = deckForUser(player1Id, player1DeckId, Faction.KINGDOM, MagicSchool.FIRE);
		Deck wrongOwnerDeck = deckForUser(otherUserId, player2DeckId, Faction.WILDCLAN, MagicSchool.FROST);

		when(deckRepository.findById(player1DeckId)).thenReturn(Optional.of(player1Deck));
		when(deckRepository.findById(player2DeckId)).thenReturn(Optional.of(wrongOwnerDeck));

		CreateGameRequest request = new CreateGameRequest();
		request.setPlayer1DeckId(player1DeckId.toString());
		request.setPlayer2Id(player2Id);
		request.setPlayer2DeckId(player2DeckId.toString());

		assertThrows(IllegalArgumentException.class, () -> gameService.createGame(player1Id, request));
	}

	@Test
	void getPlayerGamesMapsRepositoryResults() {
		String playerId = UUID.randomUUID().toString();
		Game game = new Game();
		game.setId(UUID.randomUUID());
		game.setPlayer1Id(playerId);
		game.setPlayer2Id("AI");
		game.setCurrentPlayerId(playerId);
		game.setGameStatus(GameStatus.IN_PROGRESS);
		game.setCurrentPhase(GamePhase.MAIN);
		game.setTurnNumber(3);
		game.setCreatedAt(Instant.now());
		game.setUpdatedAt(Instant.now());
		game.getPlayer1State().setUserId(playerId);
		game.getPlayer2State().setUserId("AI");

		when(gameRepository.findByPlayer1IdOrPlayer2IdOrderByUpdatedAtDesc(playerId, playerId))
			.thenReturn(List.of(game));

		var responses = gameService.getPlayerGames(playerId);

		assertEquals(1, responses.size());
		assertEquals(game.getId().toString(), responses.getFirst().getId());
	}

	@Test
	void getGameByIdReturnsEmptyWhenMissing() {
		UUID gameId = UUID.randomUUID();
		when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

		var response = gameService.getGameById(gameId);

		assertTrue(response.isEmpty());
	}

	@Test
	void getActiveGamesMapsOnlyActiveStatuses() {
		String playerId = UUID.randomUUID().toString();
		Game game = new Game();
		game.setId(UUID.randomUUID());
		game.setPlayer1Id(playerId);
		game.setPlayer2Id("AI");
		game.setCurrentPlayerId(playerId);
		game.setGameStatus(GameStatus.IN_PROGRESS);
		game.setCurrentPhase(GamePhase.MAIN);
		game.setTurnNumber(4);
		game.setCreatedAt(Instant.now());
		game.setUpdatedAt(Instant.now());
		game.getPlayer1State().setUserId(playerId);
		game.getPlayer2State().setUserId("AI");

		when(gameRepository.findByPlayerIdAndGameStatusIn(any(String.class), any(List.class))).thenReturn(List.of(game));

		var responses = gameService.getActiveGames(playerId);

		assertEquals(1, responses.size());
		assertEquals(GameStatus.IN_PROGRESS, responses.getFirst().getGameStatus());
	}

	@Test
	void getGameByIdHandlesMalformedDeckIdWithoutThrowing() {
		UUID gameId = UUID.randomUUID();
		String playerId = UUID.randomUUID().toString();
		Game game = new Game();
		game.setId(gameId);
		game.setPlayer1Id(playerId);
		game.setPlayer2Id("AI");
		game.setCurrentPlayerId(playerId);
		game.setGameStatus(GameStatus.IN_PROGRESS);
		game.setCurrentPhase(GamePhase.MAIN);
		game.setTurnNumber(1);
		game.setCreatedAt(Instant.now());
		game.setUpdatedAt(Instant.now());
		game.getPlayer1State().setUserId(playerId);
		game.getPlayer1State().setDeckId("not-a-uuid");
		game.getPlayer2State().setUserId("AI");

		when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

		var response = gameService.getGameById(gameId);

		assertTrue(response.isPresent());
		assertEquals(gameId.toString(), response.get().getId());
	}

	private static Deck deckForUser(String userId, UUID deckId, Faction faction, MagicSchool school) {
		User user = new User();
		user.setId(UUID.fromString(userId));

		Deck deck = new Deck();
		deck.setId(deckId);
		deck.setUser(user);
		deck.setName("Deck " + deckId);
		deck.setFaction(faction);
		deck.setMagicSchool(school);

		Card card = new Card();
		card.setId(UUID.randomUUID());
		card.setName("Deck Card");
		card.setCardType(CardType.CREATURE);
		card.setCost(1);
		card.setAttack(1);
		card.setHealth(1);
		card.setFaction(faction);

		deck.getDeckCards().add(new DeckCard(deck, card, 1));
		return deck;
	}
}
