package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.Game;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final Map<String, Game> activeGames;
    private final GameMatchmakingService matchmakingService;
    private final GameRules gameRules;

    public GameService(Map<String, Game> activeGames,
                       GameMatchmakingService matchmakingService,
                       GameRules gameRules) {
        this.activeGames = activeGames;
        this.matchmakingService = matchmakingService;
        this.gameRules = gameRules;
    }

    public Game joinOrCreateGame(String playerEmail) {
        return matchmakingService.joinOrCreateGame(playerEmail);
    }

    public Game processMove(String gameId, String playerEmail, int position) {
        Game game = activeGames.get(gameId);

        if (game == null) {
            throw new IllegalArgumentException("Gra nie istnieje!");
        }

        synchronized (game) {
            if (game.getStatus() != Game.GameStatus.IN_PROGRESS) {
                throw new IllegalStateException("Gra nie jest w toku!");
            }

            String playerSymbol;
            if (playerEmail.equals(game.getPlayerX())) {
                playerSymbol = "X";
            } else if (playerEmail.equals(game.getPlayerO())) {
                playerSymbol = "O";
            } else {
                throw new IllegalStateException("Nie jesteś graczem w tej grze!");
            }

            if (!game.getCurrentTurn().equals(playerSymbol)) {
                throw new IllegalStateException("To nie jest Twój ruch!");
            }
            if (game.getBoard()[position] != null) {
                throw new IllegalStateException("To pole jest już zajęte!");
            }

            game.getBoard()[position] = playerSymbol;

            if (gameRules.hasWon(game.getBoard(), playerSymbol)) {
                game.setStatus(Game.GameStatus.FINISHED);
                game.setWinner(playerEmail);
            } else if (gameRules.isBoardFull(game.getBoard())) {
                game.setStatus(Game.GameStatus.DRAW);
            } else {
                game.setCurrentTurn(playerSymbol.equals("X") ? "O" : "X");
            }

            return game;
        }
    }
}