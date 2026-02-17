package com.spellfaire.spellfairebackend.game.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spellfaire.spellfairebackend.game.model.Card;
import com.spellfaire.spellfairebackend.game.model.CardType;
import com.spellfaire.spellfairebackend.game.model.Faction;
import com.spellfaire.spellfairebackend.game.model.MagicSchool;

/**
 * Repository for Card entities.
 */
public interface CardRepository extends MongoRepository<Card, String> {
	
	List<Card> findByCardType(CardType cardType);
	
	List<Card> findByFaction(Faction faction);
	
	List<Card> findBySchool(MagicSchool school);
	
	List<Card> findByCardTypeAndFaction(CardType cardType, Faction faction);
	
	List<Card> findByCardTypeAndSchool(CardType cardType, MagicSchool school);
}
