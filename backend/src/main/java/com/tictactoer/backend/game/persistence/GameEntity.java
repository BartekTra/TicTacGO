package com.tictactoer.backend.game.persistence;

import com.tictactoer.backend.game.domain.GameMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "games")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "game_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
public abstract class GameEntity {

    @Id
    @Column(name = "game_id", nullable = false, length = 36)
    private String gameId;

    @Column(name = "player_x", nullable = true, length = 255)
    private String playerX;

    @Column(name = "player_o", length = 255)
    private String playerO;

    @Column(name = "board", nullable = false, length = 9)
    private String board;

    @Column(name = "moves_x", nullable = false, length = 255)
    private String movesX;

    @Column(name = "moves_o", nullable = false, length = 255)
    private String movesO;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private GameMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private GameStatus status;

    @Column(name = "current_turn", nullable = false, length = 1)
    private String currentTurn;

    @Column(name = "turn_started_at")
    private Instant turnStartedAt;

    @Column(name = "winner", length = 255)
    private String winner;

    @Column(name = "empty_since")
    private Instant emptySince;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "finished_by_inactivity", nullable = false)
    private boolean finishedByInactivity;

    @Version
    private Long version;

    protected GameEntity() {
        // JPA
    }

    protected GameEntity(String playerX, String playerO, GameMode mode) {
        this.gameId = UUID.randomUUID().toString();
        this.playerX = playerX;
        this.playerO = playerO;
        this.mode = mode;
        this.board = "---------";
        this.movesX = "";
        this.movesO = "";
        this.currentTurn = "X";
        this.status = GameStatus.WAITING_FOR_OPPONENT;
        this.turnStartedAt = null;
        this.emptySince = null;
        this.finishedAt = null;
        this.finishedByInactivity = false;
    }

    public void joinPlayerO(String playerO) {
        if (this.playerO != null) {
            throw new IllegalStateException("Gra jest już pełna!");
        }
        this.playerO = playerO;
        this.status = GameStatus.IN_PROGRESS;
        this.currentTurn = "X";
        this.turnStartedAt = Instant.now();
        this.emptySince = null;
        this.winner = null;
        this.finishedAt = null;
        this.finishedByInactivity = false;
    }

    public void switchTurn(Instant now) {
        this.currentTurn = this.currentTurn.equals("X") ? "O" : "X";
        this.turnStartedAt = now;
    }

    public void markFinished(String winner) {
        this.status = GameStatus.FINISHED;
        this.winner = winner;
        this.turnStartedAt = null;
        this.finishedAt = Instant.now();
        this.finishedByInactivity = false;
    }

    public void markDraw() {
        this.status = GameStatus.DRAW;
        this.winner = null;
        this.turnStartedAt = null;
        this.finishedAt = Instant.now();
        this.finishedByInactivity = false;
    }

    public void startGame(Instant now) {
        this.status = GameStatus.IN_PROGRESS;
        this.winner = null;
        this.currentTurn = "X";
        this.turnStartedAt = now;
        this.emptySince = null;
        this.finishedAt = null;
        this.finishedByInactivity = false;
    }

    public void resetBoardAndMoves() {
        this.board = "---------";
        this.movesX = "";
        this.movesO = "";
    }

    public void resetToWaitingForOpponent(Instant now) {
        this.status = GameStatus.WAITING_FOR_OPPONENT;
        this.winner = null;
        this.currentTurn = "X";
        this.turnStartedAt = null;
        resetBoardAndMoves();
        this.emptySince = (this.playerX == null && this.playerO == null) ? now : null;
        this.finishedAt = null;
        this.finishedByInactivity = false;
    }

    public void removePlayerAndReset(String playerEmail, Instant now) {
        boolean removed;
        if (playerEmail.equals(this.playerX)) {
            this.playerX = null;
            removed = true;
        } else if (playerEmail.equals(this.playerO)) {
            this.playerO = null;
            removed = true;
        } else {
            removed = false;
        }

        if (!removed) {
            throw new IllegalStateException("Nie jesteś graczem w tej grze!");
        }

        resetToWaitingForOpponent(now);
    }

    public void assignPlayers(String playerX, String playerO) {
        this.playerX = playerX;
        this.playerO = playerO;
        this.emptySince = null;
        this.winner = null;
        this.finishedAt = null;
        this.finishedByInactivity = false;
    }

    public void markFinishedByInactivity(String winner, Instant now) {
        this.status = GameStatus.FINISHED;
        this.winner = winner;
        this.turnStartedAt = null;
        this.finishedAt = now;
        this.finishedByInactivity = true;
    }

    public enum GameStatus {
        WAITING_FOR_OPPONENT,
        IN_PROGRESS,
        FINISHED,
        DRAW
    }
}

