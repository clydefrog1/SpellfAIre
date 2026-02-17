package com.spellfaire.spellfairebackend.game.repo;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.spellfaire.spellfairebackend.game.model.Game;
import com.spellfaire.spellfairebackend.game.model.GameStatus;

/**
 * Repository for Game entities.
 */
public interface GameRepository extends MongoRepository<Game, String> {
	
	List<Game> findByPlayer1IdOrPlayer2IdOrderByUpdatedAtDesc(String player1Id, String player2Id);
	
	List<Game> findByGameStatus(GameStatus gameStatus);
	
	@Query("{ $or: [ { 'player1Id': ?0 }, { 'player2Id': ?0 } ], 'gameStatus': ?1 }")
	List<Game> findByPlayerIdAndGameStatus(String playerId, GameStatus gameStatus);
	
	@Query("{ $or: [ { 'player1Id': ?0 }, { 'player2Id': ?0 } ], 'gameStatus': { $in: ?1 } }")
	List<Game> findByPlayerIdAndGameStatusIn(String playerId, List<GameStatus> statuses);
}
