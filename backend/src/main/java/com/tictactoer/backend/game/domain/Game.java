package com.tictactoer.backend.game.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
public abstract class Game {

    protected final String gameId;
    protected final String playerX;
    protected String playerO;
    protected final String[] board;
    protected final GameMode mode;

    @Setter protected String winner;
    @Setter protected String currentTurn;
    @Setter protected GameStatus status;

    public enum GameStatus {
        WAITING_FOR_OPPONENT,
        IN_PROGRESS,
        FINISHED,
        DRAW
    }

    protected Game(String playerX, GameMode mode) {
        this.gameId = UUID.randomUUID().toString();
        this.playerX = playerX;
        this.mode = mode;
        this.board = new String[9];
        this.currentTurn = "X";
        this.status = GameStatus.WAITING_FOR_OPPONENT;
    }

    public void joinPlayerO(String playerO) {
        if (this.playerO != null) {
            throw new IllegalStateException("Gra jest już pełna!");
        }
        this.playerO = playerO;
        this.status = GameStatus.IN_PROGRESS;
    }

    public void switchTurn() {
        this.currentTurn = this.currentTurn.equals("X") ? "O" : "X";
    }

    public abstract void executeMove(String playerSymbol, int position);
}