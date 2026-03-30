package com.tictactoer.backend.game.domain;

public class ClassicGame extends Game {

    public ClassicGame(String playerX) {
        super(playerX, GameMode.CLASSIC);
    }

    @Override
    public void executeMove(String playerSymbol, int position) {
        if (board[position] != null) {
            throw new IllegalStateException("To pole jest już zajęte!");
        }
        board[position] = playerSymbol;
    }
}