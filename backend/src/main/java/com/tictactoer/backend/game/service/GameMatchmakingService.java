package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.ClassicGameEntity;
import com.tictactoer.backend.game.persistence.InfiniteGameEntity;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.persistence.GameRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameMatchmakingService {

    private final GameRepository gameRepository;
    private final GameTimeoutScheduler gameTimeoutScheduler;

    @Autowired
    @Lazy
    private GameMatchmakingService self;

    public GameEntity joinOrCreateGame(String playerEmail, GameMode requestedMode) {
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return self.joinOrCreateGameAttempt(playerEmail, requestedMode);
            } catch (ConcurrencyFailureException e) {
                log.warn("Matchmaking concurrency conflict. Attempt {}/{}", i + 1, maxRetries);
                if (i == maxRetries - 1) {
                    throw new GameConcurrencyConflictException(
                            "Konflikt współbieżności podczas dołączania do gry. Spróbuj ponownie.", e);
                }
                try {
                    Thread.sleep((long) (Math.random() * 50) + 10);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new GameConcurrencyConflictException("Konflikt współbieżności.", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GameEntity joinOrCreateGameAttempt(String playerEmail, GameMode requestedMode) {
        log.info("Matchmaking: Player {} requesting game mode {}", playerEmail, requestedMode);
        Instant now = Instant.now();
        Instant emptyThreshold = now.minusSeconds(60);

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

        if (waitingWithXOpt.isPresent()) {
                GameEntity game = waitingWithXOpt.get();
                if (game.getPlayerX() != null && !game.getPlayerX().equals(playerEmail)) {
                    log.info("Matchmaking: Player {} joining existing game {} as O", playerEmail, game.getGameId());
                    game.resetBoardAndMoves();
                    game.assignPlayers(game.getPlayerX(), playerEmail);
                    game.startGame(now);
                    gameRepository.save(game);
                    gameRepository.flush();
                    gameTimeoutScheduler.scheduleTurnInactivity(
                            game.getGameId(),
                            game.getTurnStartedAt(),
                            game.getCurrentTurn()
                    );
                    return game;
                }
            }

            if (waitingWithOOpt.isPresent()) {
                GameEntity game = waitingWithOOpt.get();
                if (game.getPlayerO() != null && !game.getPlayerO().equals(playerEmail)) {
                    log.info("Matchmaking: Player {} joining existing game {} as X", playerEmail, game.getGameId());
                    game.resetBoardAndMoves();
                    game.assignPlayers(playerEmail, game.getPlayerO());
                    game.startGame(now);
                    gameRepository.save(game);
                    gameRepository.flush();
                    gameTimeoutScheduler.scheduleTurnInactivity(
                            game.getGameId(),
                            game.getTurnStartedAt(),
                            game.getCurrentTurn()
                    );
                    return game;
                }
            }

            Optional<GameEntity> emptyGameOpt = gameRepository
                    .findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNullAndEmptySinceAfter(
                            GameEntity.GameStatus.WAITING_FOR_OPPONENT,
                            requestedMode,
                            emptyThreshold
                    );

            if (emptyGameOpt.isPresent()) {
                GameEntity game = emptyGameOpt.get();
                log.info("Matchmaking: Player {} joining empty game {}", playerEmail, game.getGameId());

                boolean getsX = ThreadLocalRandom.current().nextBoolean();
                String playerX = getsX ? playerEmail : null;
                String playerO = getsX ? null : playerEmail;

                game.resetBoardAndMoves();
                game.assignPlayers(playerX, playerO);
                game.resetToWaitingForOpponent(now);

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
                    
            log.info("Matchmaking: Creating new {} game for player {}", requestedMode, playerEmail);

            gameRepository.save(newGame);
            gameRepository.flush();
            return newGame;
    }
}