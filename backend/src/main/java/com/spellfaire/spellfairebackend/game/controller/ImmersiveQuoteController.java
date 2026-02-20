package com.spellfaire.spellfairebackend.game.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spellfaire.spellfairebackend.game.dto.QuoteResponse;
import com.spellfaire.spellfairebackend.game.service.ImmersiveQuoteService;

@RestController
@RequestMapping("/api/quotes")
public class ImmersiveQuoteController {

	private final ImmersiveQuoteService immersiveQuoteService;

	public ImmersiveQuoteController(ImmersiveQuoteService immersiveQuoteService) {
		this.immersiveQuoteService = immersiveQuoteService;
	}

	@GetMapping("/random")
	public ResponseEntity<QuoteResponse> getRandomQuote() {
		return immersiveQuoteService.getRandomQuote()
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.noContent().build());
	}
}
