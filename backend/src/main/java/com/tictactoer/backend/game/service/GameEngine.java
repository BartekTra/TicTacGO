package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.GameEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GameEngine {

    public void executeMove(GameEntity game, String playerSymbol, int position) {
        if (position < 0 || position >= 9) {
            throw new IllegalArgumentException("Nieprawidłowa pozycja!");
        }

        if (game.getMode() == GameMode.CLASSIC) {
            executeClassicMove(game, playerSymbol, position);
        } else if (game.getMode() == GameMode.INFINITE) {
            executeInfiniteMove(game, playerSymbol, position);
        } else {
            throw new UnsupportedOperationException("Nieobsługiwany tryb gry");
        }
    }

    private void executeClassicMove(GameEntity game, String playerSymbol, int position) {
        StringBuilder board = new StringBuilder(game.getBoard());
        if (board.charAt(position) != '-') {
            throw new IllegalStateException("To pole jest już zajęte!");
        }

        board.setCharAt(position, playerSymbol.charAt(0));
        game.setBoard(board.toString());
    }

    private void executeInfiniteMove(GameEntity game, String playerSymbol, int position) {
        StringBuilder board = new StringBuilder(game.getBoard());
        
        List<Integer> playerMoves = parseMoves(playerSymbol.equals("X") ? game.getMovesX() : game.getMovesO());

        boolean isOverwritingOldest = playerMoves.size() == 3 && playerMoves.get(0) == position;

        if (board.charAt(position) != '-' && !isOverwritingOldest) {
            throw new IllegalStateException("To pole jest już zajęte!");
        }

        if (playerMoves.size() == 3) {
            int oldestPosition = playerMoves.remove(0);
            board.setCharAt(oldestPosition, '-');
        }

        board.setCharAt(position, playerSymbol.charAt(0));
        playerMoves.add(position);

        game.setBoard(board.toString());
        String newMoves = serializeMoves(playerMoves);
        if (playerSymbol.equals("X")) {
            game.setMovesX(newMoves);
        } else {
            game.setMovesO(newMoves);
        }
    }

    private List<Integer> parseMoves(String movesStr) {
        if (movesStr == null || movesStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(movesStr.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private String serializeMoves(List<Integer> moves) {
        return moves.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
