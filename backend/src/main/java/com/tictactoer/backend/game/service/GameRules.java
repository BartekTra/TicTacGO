package com.tictactoer.backend.game.service;

import org.springframework.stereotype.Component;

@Component
public class GameRules {

    private static final int[][] WIN_CONDITIONS = {
            {0, 1, 2},
            {3, 4, 5},
            {6, 7, 8},
            {0, 3, 6},
            {1, 4, 7},
            {2, 5, 8},
            {0, 4, 8},
            {2, 4, 6},
    };

    public boolean hasWon(String[] board, String symbol) {
        for (int[] line : WIN_CONDITIONS) {
            if (symbol.equals(board[line[0]])
                    && symbol.equals(board[line[1]])
                    && symbol.equals(board[line[2]])) {
                return true;
            }
        }
        return false;
    }

    public boolean isBoardFull(String[] board) {
        for (String cell : board) {
            if (cell == null) return false;
        }
        return true;
    }
}