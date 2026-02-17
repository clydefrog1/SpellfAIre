package com.spellfaire.spellfairebackend.game.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.repo.CardRepository;

/**
 * Initializes the database with data from JSON files on startup.
 * In development mode, drops existing collections and recreates them.
 */
@Component
public class DataInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	private final CardRepository cardRepository;
	private final MongoTemplate mongoTemplate;
	private final ObjectMapper objectMapper;

	@Value("${spellfaire.data.init.enabled:true}")
	private boolean initEnabled;

	@Value("${spellfaire.data.init.drop-existing:true}")
	private boolean dropExisting;

	public DataInitializer(
		CardRepository cardRepository,
		MongoTemplate mongoTemplate,
		ObjectMapper objectMapper
	) {
		this.cardRepository = cardRepository;
		this.mongoTemplate = mongoTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void run(String... args) throws Exception {
		if (!initEnabled) {
			log.info("Data initialization is disabled");
			return;
		}

		log.info("Starting data initialization...");

		if (dropExisting) {
			dropCollections();
		}

		loadCards();

		log.info("Data initialization complete");
	}

	/**
	 * Drop existing collections for a clean slate.
	 */
	private void dropCollections() {
		log.info("Dropping existing collections...");
		
		if (mongoTemplate.collectionExists("cards")) {
			mongoTemplate.dropCollection("cards");
			log.info("Dropped collection: cards");
		}
		
		// Optionally drop other collections for complete reset
		if (mongoTemplate.collectionExists("decks")) {
			mongoTemplate.dropCollection("decks");
			log.info("Dropped collection: decks");
		}
		
		if (mongoTemplate.collectionExists("games")) {
			mongoTemplate.dropCollection("games");
			log.info("Dropped collection: games");
		}
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
			
			cardRepository.saveAll(cards);
			log.info("Loaded {} cards", cards.size());
		}
	}
}
