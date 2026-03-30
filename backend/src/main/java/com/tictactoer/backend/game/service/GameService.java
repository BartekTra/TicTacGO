package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.persistence.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameMatchmakingService matchmakingService;
    private final GameRules gameRules;
    private final GameRepository gameRepository;

    public GameEntity joinOrCreateGame(String playerEmail, GameMode mode) {
        return matchmakingService.joinOrCreateGame(playerEmail, mode);
    }

    @Transactional
    public GameEntity processMove(String gameId, String playerEmail, int position) {
        try {
            Instant now = Instant.now();
            GameEntity game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new IllegalArgumentException("Gra nie istnieje!"));

            if (game.getStatus() != GameEntity.GameStatus.IN_PROGRESS) {
                throw new IllegalStateException("Gra nie jest w toku!");
            }

            String playerSymbol = determinePlayerSymbol(game, playerEmail);

            if (!game.getCurrentTurn().equals(playerSymbol)) {
                throw new IllegalStateException("To nie jest Twój ruch!");
            }

            game.executeMove(playerSymbol, position);

            if (gameRules.hasWon(game.getBoard(), playerSymbol)) {
                game.markFinished(playerEmail);
            } else if (game.getMode() == GameMode.CLASSIC && gameRules.isBoardFull(game.getBoard())) {
                game.markDraw();
            } else {
                game.switchTurn(now);
            }

            gameRepository.save(game);
            gameRepository.flush();
            return game;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new GameConcurrencyConflictException(
                    "Konflikt współbieżności podczas wykonywania ruchu. Spróbuj ponownie.",
                    e
            );
        }
    }

    @Transactional
    public GameEntity leaveGame(String gameId, String playerEmail) {
        Instant now = Instant.now();
        try {
            GameEntity game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new IllegalArgumentException("Gra nie istnieje!"));

            game.removePlayerAndReset(playerEmail, now);
            gameRepository.save(game);
            gameRepository.flush();
            return game;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new GameConcurrencyConflictException(
                    "Konflikt współbieżności podczas opuszczania gry. Spróbuj ponownie.",
                    e
            );
        }
    }

    private String determinePlayerSymbol(GameEntity game, String playerEmail) {
        if (playerEmail.equals(game.getPlayerX())) return "X";
        if (playerEmail.equals(game.getPlayerO())) return "O";
        throw new IllegalStateException("Nie jesteś graczem w tej grze!");
    }
}