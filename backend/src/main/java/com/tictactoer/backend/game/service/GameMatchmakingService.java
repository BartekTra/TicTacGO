package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.Game;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GameMatchmakingService {

    private final Map<String, Game> activeGames;

    public GameMatchmakingService(Map<String, Game> activeGames) {
        this.activeGames = activeGames;
    }

    public Game joinOrCreateGame(String playerEmail) {
        for (Game game : activeGames.values()) {
            if (game.getStatus() == Game.GameStatus.WAITING_FOR_OPPONENT && !game.getPlayerX().equals(playerEmail)) {
                synchronized (game) {
                    if (game.getStatus() == Game.GameStatus.WAITING_FOR_OPPONENT) {
                        game.joinPlayerO(playerEmail);
                        game.setStatus(Game.GameStatus.IN_PROGRESS);
                        return game;
                    }
                }
            }
        }

        Game newGame = new Game(playerEmail);
        activeGames.put(newGame.getGameId(), newGame);
        return newGame;
    }
}