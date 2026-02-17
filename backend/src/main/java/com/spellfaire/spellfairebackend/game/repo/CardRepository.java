package com.spellfaire.spellfairebackend.game.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;

/**
 * Repository for Card entities.
 */
public interface CardRepository extends JpaRepository<Card, UUID> {

	Optional<Card> findByName(String name);

	List<Card> findByCardType(CardType cardType);

	List<Card> findByFaction(Faction faction);

	List<Card> findBySchool(MagicSchool school);

	List<Card> findByCardTypeAndFaction(CardType cardType, Faction faction);

	List<Card> findByCardTypeAndSchool(CardType cardType, MagicSchool school);
}
