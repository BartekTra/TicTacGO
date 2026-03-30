package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.persistence.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GameCleanupScheduler {

    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void cleanupInactivity() {
        Instant threshold = Instant.now().minusSeconds(30);

        List<GameEntity> timedOutGames = gameRepository.findByStatusAndTurnStartedAtBefore(
                GameEntity.GameStatus.IN_PROGRESS,
                threshold
        );

        if (timedOutGames.isEmpty()) return;

        for (GameEntity game : timedOutGames) {
            String winner = switch (game.getCurrentTurn()) {
                case "X" -> game.getPlayerO();
                case "O" -> game.getPlayerX();
                default -> null;
            };

            game.markFinished(winner);
            messagingTemplate.convertAndSend(
                    "/topic/game." + game.getGameId(),
                    game
            );
        }

        gameRepository.deleteAllInBatch(timedOutGames);
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void cleanupEmptyGames() {
        Instant threshold = Instant.now().minusSeconds(60);

        List<GameEntity> emptyGames = gameRepository.findByPlayerXIsNullAndPlayerOIsNullAndEmptySinceBefore(
                threshold
        );

        if (emptyGames.isEmpty()) return;

        gameRepository.deleteAllInBatch(emptyGames);
    }
}

