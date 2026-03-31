package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.persistence.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GameTimeoutScheduler {

    private static final long TURN_INACTIVITY_SECONDS = 30;
    private static final long INACTIVITY_DELETE_AFTER_SECONDS = 10;
    private static final long EMPTY_DELETE_SECONDS = 60;

    private final TaskScheduler taskScheduler;
    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TransactionTemplate transactionTemplate;

    public void scheduleEmptyDeletion(String gameId, Instant expectedEmptySince) {
        if (expectedEmptySince == null) return;

        Instant deletionAt = expectedEmptySince.plusSeconds(EMPTY_DELETE_SECONDS);
        taskScheduler.schedule(() -> deleteIfEmpty(gameId, expectedEmptySince), deletionAt);
    }

    public void scheduleTurnInactivity(String gameId, Instant expectedTurnStartedAt, String expectedCurrentTurn) {
        if (expectedTurnStartedAt == null || expectedCurrentTurn == null) return;

        Instant winAt = expectedTurnStartedAt.plusSeconds(TURN_INACTIVITY_SECONDS);
        taskScheduler.schedule(
                () -> resolveInactivityWin(gameId, expectedTurnStartedAt, expectedCurrentTurn),
                winAt
        );
    }

    private record InactivityResolution(long finishedAtMillis, String winnerEmail) {}

    protected void resolveInactivityWin(String gameId, Instant expectedTurnStartedAt, String expectedCurrentTurn) {
        InactivityResolution resolution = transactionTemplate.execute(status -> {
            Optional<GameEntity> gameOpt = gameRepository.findById(gameId);
            if (gameOpt.isEmpty()) return null;

            GameEntity game = gameOpt.get();

            if (game.getStatus() != GameEntity.GameStatus.IN_PROGRESS) return null;
            if (game.getPlayerX() == null || game.getPlayerO() == null) return null;

            if (game.getTurnStartedAt() == null) return null;
            if (!game.getTurnStartedAt().equals(expectedTurnStartedAt)) return null;
            if (!expectedCurrentTurn.equals(game.getCurrentTurn())) return null;

            String winnerEmail =
                    expectedCurrentTurn.equals("X") ? game.getPlayerO() : game.getPlayerX();

            Instant finishedAt = Instant.now();
            game.markFinishedByInactivity(winnerEmail, finishedAt);
            gameRepository.saveAndFlush(game);

            messagingTemplate.convertAndSend("/topic/game." + game.getGameId(), game);

            return new InactivityResolution(finishedAt.toEpochMilli(), winnerEmail);
        });

        if (resolution == null) return;

        Instant deleteAt = Instant.ofEpochMilli(resolution.finishedAtMillis()).plusSeconds(INACTIVITY_DELETE_AFTER_SECONDS);
        taskScheduler.schedule(
                () -> deleteIfFinishedByInactivity(gameId, resolution.finishedAtMillis(), resolution.winnerEmail()),
                deleteAt
        );
    }

    protected void deleteIfFinishedByInactivity(
            String gameId,
            long expectedFinishedAtMillis,
            String expectedWinnerEmail
    ) {
        transactionTemplate.execute(status -> {
            Optional<GameEntity> gameOpt = gameRepository.findById(gameId);
            if (gameOpt.isEmpty()) return null;

            GameEntity game = gameOpt.get();
            if (game.getStatus() != GameEntity.GameStatus.FINISHED) return null;
            if (!game.isFinishedByInactivity()) return null;
            if (game.getFinishedAt() == null) return null;
            if (game.getFinishedAt().toEpochMilli() != expectedFinishedAtMillis) return null;
            if (expectedWinnerEmail != null && !expectedWinnerEmail.equals(game.getWinner())) return null;

            gameRepository.delete(game);
            return null;
        });
    }

    protected void deleteIfEmpty(String gameId, Instant expectedEmptySince) {
        transactionTemplate.execute(status -> {
            Optional<GameEntity> gameOpt = gameRepository.findById(gameId);
            if (gameOpt.isEmpty()) return null;

            GameEntity game = gameOpt.get();
            if (game.getPlayerX() != null || game.getPlayerO() != null) return null;
            if (game.getEmptySince() == null) return null;
            if (!game.getEmptySince().equals(expectedEmptySince)) return null;

            gameRepository.delete(game);
            return null;
        });
    }
}

