package com.tictactoer.backend.game.service;

public class GameConcurrencyConflictException extends RuntimeException {

    public GameConcurrencyConflictException(String message) {
        super(message);
    }

    public GameConcurrencyConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

