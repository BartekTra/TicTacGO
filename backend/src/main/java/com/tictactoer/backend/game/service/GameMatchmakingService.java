package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.ClassicGame;
import com.tictactoer.backend.game.domain.Game;
import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.domain.InfiniteGame;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GameMatchmakingService {

    private final Map<String, Game> activeGames;

    public GameMatchmakingService(Map<String, Game> activeGames) {
        this.activeGames = activeGames;
    }

    public Game joinOrCreateGame(String playerEmail, GameMode requestedMode) {
        for (Game game : activeGames.values()) {
            if (game.getStatus() == Game.GameStatus.WAITING_FOR_OPPONENT
                    && game.getMode() == requestedMode
                    && !game.getPlayerX().equals(playerEmail)) {
                synchronized (game) {
                    if (game.getStatus() == Game.GameStatus.WAITING_FOR_OPPONENT) {
                        game.joinPlayerO(playerEmail);
                        return game;
                    }
                }
            }
        }

        Game newGame = requestedMode == GameMode.CLASSIC
                ? new ClassicGame(playerEmail)
                : new InfiniteGame(playerEmail);

        activeGames.put(newGame.getGameId(), newGame);
        return newGame;
    }
}