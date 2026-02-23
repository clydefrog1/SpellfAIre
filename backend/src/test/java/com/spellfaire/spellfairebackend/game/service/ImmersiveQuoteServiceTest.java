package com.spellfaire.spellfairebackend.game.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.spellfaire.spellfairebackend.game.model.ImmersiveQuote;
import com.spellfaire.spellfairebackend.game.repo.ImmersiveQuoteRepository;

@ExtendWith(MockitoExtension.class)
class ImmersiveQuoteServiceTest {

	@Mock
	private ImmersiveQuoteRepository immersiveQuoteRepository;

	private ImmersiveQuoteService quoteService;

	@BeforeEach
	void setUp() {
		quoteService = new ImmersiveQuoteService(immersiveQuoteRepository);
	}

	@Test
	void getRandomQuoteReturnsEmptyWhenNoQuotesExist() {
		when(immersiveQuoteRepository.count()).thenReturn(0L);

		Optional<?> result = quoteService.getRandomQuote();

		assertTrue(result.isEmpty());
	}

	@Test
	void getRandomQuoteReturnsMappedQuoteWhenSingleRecordExists() {
		ImmersiveQuote quote = new ImmersiveQuote();
		quote.setText("Magic bends to the bold.");

		when(immersiveQuoteRepository.count()).thenReturn(1L);
		when(immersiveQuoteRepository.findAll(PageRequest.of(0, 1, Sort.by("id"))))
			.thenReturn(new PageImpl<>(List.of(quote)));

		var result = quoteService.getRandomQuote();

		assertTrue(result.isPresent());
		assertEquals("Magic bends to the bold.", result.get().getText());
	}
}
