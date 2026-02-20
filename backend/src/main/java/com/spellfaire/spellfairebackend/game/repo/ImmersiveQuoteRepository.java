package com.spellfaire.spellfairebackend.game.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spellfaire.spellfairebackend.game.model.ImmersiveQuote;

public interface ImmersiveQuoteRepository extends JpaRepository<ImmersiveQuote, UUID> {
}
