package com.tictactoer.backend.game.controller.dto;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.GameEntity;
import java.time.Instant;

public record GameResponseDTO(
        String gameId,
        String playerX,
        String playerO,
        String board,
        GameMode mode,
        String status,
        String currentTurn,
        Instant turnStartedAt,
        String winner
) {
    public static GameResponseDTO fromEntity(GameEntity entity) {
        return new GameResponseDTO(
                entity.getGameId(),
                entity.getPlayerX(),
                entity.getPlayerO(),
                entity.getBoard(),
                entity.getMode(),
                entity.getStatus().name(),
                entity.getCurrentTurn(),
                entity.getTurnStartedAt(),
                entity.getWinner()
        );
    }
}
