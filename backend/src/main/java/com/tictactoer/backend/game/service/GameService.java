package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.Game;
import com.tictactoer.backend.game.domain.GameMode;
import org.springframework.stereotype.Service;

import java.util.Map;

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

    public Game joinOrCreateGame(String playerEmail, GameMode mode) {
        return matchmakingService.joinOrCreateGame(playerEmail, mode);
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

            String playerSymbol = determinePlayerSymbol(game, playerEmail);

            if (!game.getCurrentTurn().equals(playerSymbol)) {
                throw new IllegalStateException("To nie jest Twój ruch!");
            }

            game.executeMove(playerSymbol, position);

            if (gameRules.hasWon(game.getBoard(), playerSymbol)) {
                game.setStatus(Game.GameStatus.FINISHED);
                game.setWinner(playerEmail);
            } else if (game.getMode() == GameMode.CLASSIC && gameRules.isBoardFull(game.getBoard())) {
                game.setStatus(Game.GameStatus.DRAW);
            } else {
                game.switchTurn();
            }

            return game;
        }
    }

    private String determinePlayerSymbol(Game game, String playerEmail) {
        if (playerEmail.equals(game.getPlayerX())) return "X";
        if (playerEmail.equals(game.getPlayerO())) return "O";
        throw new IllegalStateException("Nie jesteś graczem w tej grze!");
    }
}