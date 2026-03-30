package com.tictactoer.backend.game.domain;

import lombok.Setter;

import java.util.UUID;

public class Game {

    private final String gameId;
    private final String playerX;
    private final String[] board;

    @Setter
    private String winner;
    private String playerO;
    @Setter
    private String currentTurn;
    @Setter
    private GameStatus status;

    public enum GameStatus {
        WAITING_FOR_OPPONENT,
        IN_PROGRESS,
        FINISHED,
        DRAW
    }

    public Game(String playerX) {
        this.gameId = UUID.randomUUID().toString();
        this.playerX = playerX;
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

    public String getGameId() {
        return gameId;
    }

    public String getPlayerX() {
        return playerX;
    }

    public String getPlayerO() {
        return playerO;
    }

    public String[] getBoard() {
        return board;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public GameStatus getStatus() {
        return status;
    }

    public String getWinner() {return winner;}

}