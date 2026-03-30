package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.ClassicGameEntity;
import com.tictactoer.backend.game.persistence.InfiniteGameEntity;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.persistence.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class GameMatchmakingService {

    private final GameRepository gameRepository;

    @Transactional
    public GameEntity joinOrCreateGame(String playerEmail, GameMode requestedMode) {
        Instant now = Instant.now();

        Optional<GameEntity> waitingWithXOpt = gameRepository
                .findFirstByStatusAndModeAndPlayerOIsNullAndPlayerXIsNotNull(
                        GameEntity.GameStatus.WAITING_FOR_OPPONENT,
                        requestedMode
                );

        Optional<GameEntity> waitingWithOOpt = gameRepository
                .findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNotNull(
                        GameEntity.GameStatus.WAITING_FOR_OPPONENT,
                        requestedMode
                );

        try {
            GameEntity waitingGame = null;

            if (waitingWithXOpt.isPresent()) {
                GameEntity candidate = waitingWithXOpt.get();
                String existingPlayer =
                        candidate.getPlayerX() != null ? candidate.getPlayerX() : candidate.getPlayerO();
                if (!playerEmail.equals(existingPlayer)) {
                    waitingGame = candidate;
                }
            }

            if (waitingGame == null && waitingWithOOpt.isPresent()) {
                GameEntity candidate = waitingWithOOpt.get();
                String existingPlayer =
                        candidate.getPlayerX() != null ? candidate.getPlayerX() : candidate.getPlayerO();
                if (!playerEmail.equals(existingPlayer)) {
                    waitingGame = candidate;
                }
            }

            if (waitingGame != null) {
                GameEntity game = waitingGame;

                // W stanie WAITING plansza i historia ruchów powinny być puste (po opuszczeniu/reset),
                // więc możemy losowo przypisać znaki przez ewentualną zamianę slotów.
                game.resetBoardAndMoves();

                boolean joinerGetsX = ThreadLocalRandom.current().nextBoolean();
                String existing = game.getPlayerX() != null ? game.getPlayerX() : game.getPlayerO();

                if (joinerGetsX) {
                    game.assignPlayers(playerEmail, existing);
                } else {
                    game.assignPlayers(existing, playerEmail);
                }

                game.startGame(now);
                gameRepository.save(game);
                gameRepository.flush();
                return game;
            }

            boolean firstGetsX = ThreadLocalRandom.current().nextBoolean();
            String playerX = firstGetsX ? playerEmail : null;
            String playerO = firstGetsX ? null : playerEmail;

            GameEntity newGame = requestedMode == GameMode.CLASSIC
                    ? new ClassicGameEntity(playerX, playerO)
                    : new InfiniteGameEntity(playerX, playerO);

            gameRepository.save(newGame);
            gameRepository.flush();
            return newGame;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new GameConcurrencyConflictException(
                    "Konflikt współbieżności podczas dołączania do gry. Spróbuj ponownie.",
                    e
            );
        }
    }
}