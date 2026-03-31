package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.persistence.GameRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameMatchmakingService matchmakingService;
    private final GameEngine gameEngine;
    private final GameRules gameRules;
    private final GameRepository gameRepository;
    private final GameTimeoutScheduler gameTimeoutScheduler;

    public GameEntity joinOrCreateGame(String playerEmail, GameMode mode) {
        log.info("Player {} joining/creating game with mode {}", playerEmail, mode);
        return matchmakingService.joinOrCreateGame(playerEmail, mode);
    }

    @Transactional
    public GameEntity processMove(String gameId, String playerEmail, int position) {
        log.info("Player {} attempting move at position {} in game {}", playerEmail, position, gameId);
        try {
            Instant now = Instant.now();
            GameEntity game = gameRepository.findById(gameId)
                    .orElseThrow(() -> {
                        log.warn("Game processing move failed: Game {} not found", gameId);
                        return new IllegalArgumentException("Gra nie istnieje!");
                    });

            if (game.getStatus() != GameEntity.GameStatus.IN_PROGRESS) {
                log.warn("Game processing move failed: Game {} is not in progress", gameId);
                throw new IllegalStateException("Gra nie jest w toku!");
            }

            String playerSymbol = determinePlayerSymbol(game, playerEmail);

            if (!game.getCurrentTurn().equals(playerSymbol)) {
                log.warn("Game processing move failed: Not player {}'s turn in game {}", playerEmail, gameId);
                throw new IllegalStateException("To nie jest Twój ruch!");
            }

            gameEngine.executeMove(game, playerSymbol, position);

            if (gameRules.hasWon(game.getBoard(), playerSymbol)) {
                game.markFinished(playerEmail);
            } else if (game.getMode() == GameMode.CLASSIC && gameRules.isBoardFull(game.getBoard())) {
                game.markDraw();
            } else {
                game.switchTurn(now);
            }

            gameRepository.save(game);
            gameRepository.flush();

            if (game.getStatus() == GameEntity.GameStatus.IN_PROGRESS
                    && game.getPlayerX() != null
                    && game.getPlayerO() != null
                    && game.getTurnStartedAt() != null) {
                gameTimeoutScheduler.scheduleTurnInactivity(
                        game.getGameId(),
                        game.getTurnStartedAt(),
                        game.getCurrentTurn()
                );
            }

            return game;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Concurrency conflict during move for player {} in game {}", playerEmail, gameId);
            throw new GameConcurrencyConflictException(
                    "Konflikt współbieżności podczas wykonywania ruchu. Spróbuj ponownie.",
                    e
            );
        }
    }

    @Transactional
    public GameEntity leaveGame(String gameId, String playerEmail) {
        log.info("Player {} leaving game {}", playerEmail, gameId);
        Instant now = Instant.now();
        try {
            GameEntity game = gameRepository.findById(gameId)
                    .orElseThrow(() -> {
                        log.warn("Leave game failed: Game {} not found", gameId);
                        return new IllegalArgumentException("Gra nie istnieje!");
                    });

            game.removePlayerAndReset(playerEmail, now);
            gameRepository.save(game);
            gameRepository.flush();

            if (game.getPlayerX() == null && game.getPlayerO() == null) {
                gameTimeoutScheduler.scheduleEmptyDeletion(game.getGameId(), game.getEmptySince());
            }

            return game;
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Concurrency conflict while player {} leaving game {}", playerEmail, gameId);
            throw new GameConcurrencyConflictException(
                    "Konflikt współbieżności podczas opuszczania gry. Spróbuj ponownie.",
                    e
            );
        }
    }

    private String determinePlayerSymbol(GameEntity game, String playerEmail) {
        if (playerEmail.equals(game.getPlayerX())) return "X";
        if (playerEmail.equals(game.getPlayerO())) return "O";
        log.warn("Player {} is not a participant in game {}", playerEmail, game.getGameId());
        throw new IllegalStateException("Nie jesteś graczem w tej grze!");
    }
}