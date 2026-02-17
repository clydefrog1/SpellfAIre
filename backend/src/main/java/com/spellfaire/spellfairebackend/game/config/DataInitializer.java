package com.spellfaire.spellfairebackend.game.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;
import com.spellfaire.spellfairebackend.game.repo.DeckRepository;
import com.spellfaire.spellfairebackend.game.repo.GameRepository;

/**
 * Initializes the database with data from JSON files on startup.
 * In development mode, clears existing tables and recreates them.
 */
@Component
public class DataInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	private final CardRepository cardRepository;
	private final DeckRepository deckRepository;
	private final GameRepository gameRepository;
	private final ObjectMapper objectMapper;

	@Value("${spellfaire.data.init.enabled:true}")
	private boolean initEnabled;

	@Value("${spellfaire.data.init.drop-existing:true}")
	private boolean dropExisting;

	public DataInitializer(
		CardRepository cardRepository,
		DeckRepository deckRepository,
		GameRepository gameRepository,
		ObjectMapper objectMapper
	) {
		this.cardRepository = cardRepository;
		this.deckRepository = deckRepository;
		this.gameRepository = gameRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		if (!initEnabled) {
			log.info("Data initialization is disabled");
			return;
		}

		log.info("Starting data initialization...");

		if (dropExisting) {
			clearTables();
		}

		loadCards();

		log.info("Data initialization complete");
	}

	/**
	 * Clear existing tables for a clean slate.
	 * Order matters due to foreign key constraints.
	 */
	private void clearTables() {
		log.info("Clearing existing tables...");

		gameRepository.deleteAll();
		log.info("Cleared table: games");

		deckRepository.deleteAll();
		log.info("Cleared table: decks");

		cardRepository.deleteAll();
		log.info("Cleared table: cards");
	}

	/**
	 * Load cards from JSON file.
	 */
	private void loadCards() throws IOException {
		log.info("Loading cards from JSON...");

		ClassPathResource resource = new ClassPathResource("data/cards.json");

		if (!resource.exists()) {
			log.warn("Cards data file not found at: data/cards.json");
			return;
		}

		try (InputStream inputStream = resource.getInputStream()) {
			List<Card> cards = objectMapper.readValue(
				inputStream,
				new TypeReference<List<Card>>() {}
			);

			// Ensure IDs are null so JPA generates UUIDs
			cards.forEach(card -> card.setId(null));

			cardRepository.saveAll(cards);
			log.info("Loaded {} cards", cards.size());
		}
	}
}
