package com.spellfaire.spellfairebackend.game.repo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.spellfaire.spellfairebackend.game.model.Game;
import com.spellfaire.spellfairebackend.game.model.GameStatus;

/**
 * Repository for Game entities.
 */
public interface GameRepository extends JpaRepository<Game, UUID> {

	List<Game> findByPlayer1IdOrPlayer2IdOrderByUpdatedAtDesc(String player1Id, String player2Id);

	List<Game> findByGameStatus(GameStatus gameStatus);

	@Query("SELECT g FROM Game g WHERE (g.player1Id = :playerId OR g.player2Id = :playerId) AND g.gameStatus = :status")
	List<Game> findByPlayerIdAndGameStatus(@Param("playerId") String playerId, @Param("status") GameStatus status);

	@Query("SELECT g FROM Game g WHERE (g.player1Id = :playerId OR g.player2Id = :playerId) AND g.gameStatus IN :statuses")
	List<Game> findByPlayerIdAndGameStatusIn(@Param("playerId") String playerId, @Param("statuses") List<GameStatus> statuses);
}
