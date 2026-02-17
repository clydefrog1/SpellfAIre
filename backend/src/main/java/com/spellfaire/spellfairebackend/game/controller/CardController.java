package com.spellfaire.spellfairebackend.game.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spellfaire.spellfairebackend.game.dto.CardResponse;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;
import com.spellfaire.spellfairebackend.game.service.CardService;

/**
 * REST controller for card-related operations.
 * Cards are static game data available to all players.
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

	private final CardService cardService;

	public CardController(CardService cardService) {
		this.cardService = cardService;
	}

	/**
	 * Get all cards.
	 * Can be filtered by faction or school.
	 */
	@GetMapping
	public ResponseEntity<List<CardResponse>> getAllCards(
		@RequestParam(required = false) Faction faction,
		@RequestParam(required = false) MagicSchool school
	) {
		if (faction != null) {
			return ResponseEntity.ok(cardService.getCreaturesByFaction(faction));
		}
		if (school != null) {
			return ResponseEntity.ok(cardService.getSpellsBySchool(school));
		}
		return ResponseEntity.ok(cardService.getAllCards());
	}

	/**
	 * Get a specific card by ID.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<CardResponse> getCardById(@PathVariable String id) {
		return cardService.getCardById(UUID.fromString(id))
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Get all creature cards.
	 */
	@GetMapping("/creatures")
	public ResponseEntity<List<CardResponse>> getCreatures() {
		return ResponseEntity.ok(cardService.getCreatures());
	}

	/**
	 * Get all spell cards.
	 */
	@GetMapping("/spells")
	public ResponseEntity<List<CardResponse>> getSpells() {
		return ResponseEntity.ok(cardService.getSpells());
	}
}
