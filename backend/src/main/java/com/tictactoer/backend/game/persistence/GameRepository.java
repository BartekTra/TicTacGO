package com.tictactoer.backend.game.persistence;

import com.tictactoer.backend.game.domain.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<GameEntity, String> {

    Optional<GameEntity> findFirstByStatusAndModeAndPlayerOIsNullAndPlayerXIsNotNull(
            GameEntity.GameStatus status,
            GameMode mode
    );

    Optional<GameEntity> findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNotNull(
            GameEntity.GameStatus status,
            GameMode mode
    );

    List<GameEntity> findByStatusAndTurnStartedAtBefore(
            GameEntity.GameStatus status,
            Instant threshold
    );

    List<GameEntity> findByPlayerXIsNullAndPlayerOIsNullAndEmptySinceBefore(Instant threshold);
}

