package com.tictactoer.backend.game.persistence;

import com.tictactoer.backend.game.domain.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<GameEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GameEntity> findFirstByStatusAndModeAndPlayerOIsNullAndPlayerXIsNotNull(
            GameEntity.GameStatus status,
            GameMode mode
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GameEntity> findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNotNull(
            GameEntity.GameStatus status,
            GameMode mode
    );

    List<GameEntity> findByStatusAndTurnStartedAtBefore(
            GameEntity.GameStatus status,
            Instant threshold
    );

    List<GameEntity> findByPlayerXIsNullAndPlayerOIsNullAndEmptySinceBefore(Instant threshold);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GameEntity> findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNullAndEmptySinceAfter(
            GameEntity.GameStatus status,
            GameMode mode,
            Instant threshold
    );
}

