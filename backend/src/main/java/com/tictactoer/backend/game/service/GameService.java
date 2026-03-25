package com.tictactoer.backend.game.service;
import com.tictactoer.backend.game.domain.Game;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();

    public Game processMove(String gameId, String player, int position) {
        Game game = activeGames.get(gameId);

        if (game == null) {
            throw new IllegalArgumentException("Gra nie istnieje!");
        }

        synchronized (game) {
            if (!game.getCurrentTurn().equals(player)) {
                throw new IllegalStateException("To nie jest Twój ruch!");
            }
            if (game.getBoard()[position] != null) {
                throw new IllegalStateException("To pole jest już zajęte!");
            }

            game.getBoard()[position] = player;

            // checkWinCondition(game);

            game.setCurrentTurn(player.equals("X") ? "O" : "X");

            return game;
        }
    }
}