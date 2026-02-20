package com.spellfaire.spellfairebackend.game.service;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.spellfaire.spellfairebackend.game.dto.QuoteResponse;
import com.spellfaire.spellfairebackend.game.model.ImmersiveQuote;
import com.spellfaire.spellfairebackend.game.repo.ImmersiveQuoteRepository;

@Service
public class ImmersiveQuoteService {

	private final ImmersiveQuoteRepository immersiveQuoteRepository;

	public ImmersiveQuoteService(ImmersiveQuoteRepository immersiveQuoteRepository) {
		this.immersiveQuoteRepository = immersiveQuoteRepository;
	}

	public Optional<QuoteResponse> getRandomQuote() {
		long count = immersiveQuoteRepository.count();
		if (count <= 0) {
			return Optional.empty();
		}

		int bound = Math.toIntExact(count);
		int index = ThreadLocalRandom.current().nextInt(bound);

		return immersiveQuoteRepository
			.findAll(PageRequest.of(index, 1, Sort.by("id")))
			.stream()
			.findFirst()
			.map(this::toResponse);
	}

	private QuoteResponse toResponse(ImmersiveQuote quote) {
		QuoteResponse resp = new QuoteResponse();
		resp.setText(quote.getText());
		return resp;
	}
}
