package com.spellfaire.spellfairebackend.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;
import com.spellfaire.spellfairebackend.game.repo.DeckRepository;
import com.spellfaire.spellfairebackend.game.repo.GameRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class GameControllerIT {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Container
	@SuppressWarnings("resource")
	private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("spellfaire_test")
			.withUsername("test")
			.withPassword("test");

	@DynamicPropertySource
	static void configureDatasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
	}

	@LocalServerPort
	private int port;

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private DeckRepository deckRepository;

	@Autowired
	private GameRepository gameRepository;

	@BeforeEach
	void setUp() {
		gameRepository.deleteAll();
		deckRepository.deleteAll();
		cardRepository.deleteAll();
		seedCardPool();
	}

	@Test
	void createAiGameRequiresAuthentication() throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/games/ai")))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"faction\":\"KINGDOM\",\"magicSchool\":\"FIRE\"}"))
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(403, response.statusCode());
	}

	@Test
	@SuppressWarnings("unchecked")
	void createAiGameReturnsCreatedGameState() throws Exception {
		String accessToken = registerAndGetAccessToken();

		Map<String, Object> body = Map.of(
				"faction", "KINGDOM",
				"magicSchool", "FIRE");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/games/ai")))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(201, response.statusCode());

		Map<String, Object> payload = OBJECT_MAPPER.readValue(response.body(), Map.class);
		Map<String, Object> game = (Map<String, Object>) payload.get("game");

		assertNotNull(game.get("id"));
		assertEquals("IN_PROGRESS", game.get("gameStatus"));
		assertEquals("MAIN", game.get("currentPhase"));

		Map<String, Object> player1State = (Map<String, Object>) game.get("player1State");
		assertNotNull(player1State.get("deckId"));
		assertNotNull(player1State.get("hand"));
		assertEquals("KINGDOM", player1State.get("faction"));
		assertEquals("FIRE", player1State.get("magicSchool"));

		Map<String, Object> player2State = (Map<String, Object>) game.get("player2State");
		assertNotNull(player2State.get("deckId"));
		assertNotNull(player2State.get("faction"));
		assertNotNull(player2State.get("magicSchool"));
		assertNotEquals("KINGDOM", player2State.get("faction"));
		assertNotEquals("FIRE", player2State.get("magicSchool"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void playCardReturnsBadRequestWhenCardNotInHand() throws Exception {
		String accessToken = registerAndGetAccessToken();
		Map<String, Object> createPayload = createAiGame(accessToken);

		Map<String, Object> game = (Map<String, Object>) createPayload.get("game");
		String gameId = (String) game.get("id");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/games/" + gameId + "/play-card")))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(
						"{\"cardId\":\"" + UUID.randomUUID() + "\"}"))
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(400, response.statusCode());
		assertTrue(response.body().contains("Card not in hand"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void attackReturnsBadRequestWhenAttackerDoesNotExist() throws Exception {
		String accessToken = registerAndGetAccessToken();
		Map<String, Object> createPayload = createAiGame(accessToken);

		Map<String, Object> game = (Map<String, Object>) createPayload.get("game");
		String gameId = (String) game.get("id");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/games/" + gameId + "/attack")))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(
						"{\"attackerInstanceId\":\"" + UUID.randomUUID() + "\",\"targetId\":\"ENEMY_HERO\"}"))
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(400, response.statusCode());
		assertTrue(response.body().contains("Attacker not found"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void endTurnReturnsBadRequestForUserNotInGame() throws Exception {
		String ownerToken = registerAndGetAccessToken();
		String strangerToken = registerAndGetAccessToken();
		Map<String, Object> createPayload = createAiGame(ownerToken);

		Map<String, Object> game = (Map<String, Object>) createPayload.get("game");
		String gameId = (String) game.get("id");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/games/" + gameId + "/end-turn")))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken)
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(400, response.statusCode());
		assertTrue(response.body().contains("Not your turn"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void surrenderSetsFinishedStatusAndAiAsWinner() throws Exception {
		String accessToken = registerAndGetAccessToken();
		Map<String, Object> createPayload = createAiGame(accessToken);

		Map<String, Object> gameMap = (Map<String, Object>) createPayload.get("game");
		String gameId = (String) gameMap.get("id");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/games/" + gameId + "/surrender")))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, response.statusCode());

		Map<String, Object> payload = OBJECT_MAPPER.readValue(response.body(), Map.class);
		Map<String, Object> updatedGame = (Map<String, Object>) payload.get("game");
		assertEquals("FINISHED", updatedGame.get("gameStatus"));
		assertEquals("AI", updatedGame.get("winnerId"));
	}

	private Map<String, Object> createAiGame(String accessToken) throws Exception {
		Map<String, Object> body = Map.of(
				"faction", "KINGDOM",
				"magicSchool", "FIRE");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/games/ai")))
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(201, response.statusCode());
		return OBJECT_MAPPER.readValue(response.body(), Map.class);
	}

	@SuppressWarnings("unchecked")
	private String registerAndGetAccessToken() throws Exception {
		String email = "game.test." + Instant.now().toEpochMilli() + "@example.com";
		Map<String, Object> body = Map.of(
				"email", email,
				"username", "GameTester",
				"password", "password-123");

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url("/api/auth/register")))
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body)))
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, response.statusCode());

		Map<String, Object> payload = OBJECT_MAPPER.readValue(response.body(), Map.class);
		return (String) payload.get("accessToken");
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}

	private void seedCardPool() {
		List<Card> cards = new ArrayList<>();

		for (Faction faction : Faction.values()) {
			for (int index = 1; index <= 7; index++) {
				Card card = new Card();
				card.setName("IT " + faction.name() + " Creature " + index + " " + UUID.randomUUID());
				card.setCardType(CardType.CREATURE);
				card.setCost(index);
				card.setAttack(1 + index);
				card.setHealth(2 + index);
				card.setFaction(faction);
				cards.add(card);
			}
		}

		for (MagicSchool school : MagicSchool.values()) {
			for (int index = 1; index <= 6; index++) {
				Card card = new Card();
				card.setName("IT " + school.name() + " Spell " + index + " " + UUID.randomUUID());
				card.setCardType(CardType.SPELL);
				card.setCost(index);
				card.setSchool(school);
				cards.add(card);
			}
		}

		cardRepository.saveAll(cards);
	}
}
