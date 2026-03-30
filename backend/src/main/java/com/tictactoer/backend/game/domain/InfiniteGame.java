package com.tictactoer.backend.game.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedList;
import java.util.Queue;

public class InfiniteGame extends Game {

    @JsonIgnore
    private final Queue<Integer> movesX = new LinkedList<>();

    @JsonIgnore
    private final Queue<Integer> movesO = new LinkedList<>();

    public InfiniteGame(String playerX) {
        super(playerX, GameMode.INFINITE);
    }

    @Override
    public void executeMove(String playerSymbol, int position) {
        Queue<Integer> playerMoves = playerSymbol.equals("X") ? movesX : movesO;

        boolean isOverwritingOldest = (playerMoves.size() == 3 && playerMoves.peek() == position);

        if (board[position] != null && !isOverwritingOldest) {
            throw new IllegalStateException("To pole jest już zajęte!");
        }

        if (playerMoves.size() == 3) {
            int oldestPosition = playerMoves.poll();
            board[oldestPosition] = null;
        }

        board[position] = playerSymbol;
        playerMoves.add(position);
    }
}