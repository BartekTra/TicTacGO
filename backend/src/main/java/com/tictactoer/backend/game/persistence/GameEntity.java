package com.tictactoer.backend.game.persistence;

import com.tictactoer.backend.game.domain.GameMode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.AccessLevel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "games")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "game_type", discriminatorType = DiscriminatorType.STRING)
@Getter
public abstract class


GameEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @Column(name = "game_id", nullable = false, length = 36)
    private String gameId;

    @Column(name = "player_x", nullable = true, length = 255)
    private String playerX;

    @Column(name = "player_o", length = 255)
    private String playerO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "board", nullable = false)
    @JsonIgnore
    private String boardJson;

    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    protected String[] boardCells;

    @JdbcTypeCode(SqlTypes.JSON)
    @JsonIgnore
    @Column(name = "moves_x", nullable = false)
    private String movesXJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @JsonIgnore
    @Column(name = "moves_o", nullable = false)
    private String movesOJson;

    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    protected List<Integer> movesX;

    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    protected List<Integer> movesO;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private GameMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private GameStatus status;

    @Column(name = "current_turn", nullable = false, length = 1)
    private String currentTurn;

    @JsonIgnore
    @Column(name = "turn_started_at")
    private Instant turnStartedAt;

    @Column(name = "winner", length = 255)
    private String winner;

    @JsonIgnore
    @Column(name = "empty_since")
    private Instant emptySince;

    @Version
    @JsonIgnore
    private Long version;

    protected GameEntity() {
        // JPA
    }

    protected GameEntity(String playerX, String playerO, GameMode mode) {
        this.gameId = UUID.randomUUID().toString();
        this.playerX = playerX;
        this.playerO = playerO;
        this.mode = mode;
        this.boardCells = new String[9];
        this.boardJson = toJson(boardCells);
        this.movesX = new ArrayList<>(3);
        this.movesO = new ArrayList<>(3);
        this.movesXJson = toJsonIntList(movesX);
        this.movesOJson = toJsonIntList(movesO);
        this.currentTurn = "X";
        this.status = GameStatus.WAITING_FOR_OPPONENT;
        this.turnStartedAt = null;
        this.emptySince = null;
    }

    @PostLoad
    protected void onPostLoad() {
        this.boardCells = fromJson(boardJson);
        this.movesX = fromJsonIntList(movesXJson);
        this.movesO = fromJsonIntList(movesOJson);
    }

    public String[] getBoard() {
        return boardCells;
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
    }

    public void switchTurn(Instant now) {
        this.currentTurn = this.currentTurn.equals("X") ? "O" : "X";
        this.turnStartedAt = now;
    }

    public void markFinished(String winner) {
        this.status = GameStatus.FINISHED;
        this.winner = winner;
        this.turnStartedAt = null;
    }

    public void markDraw() {
        this.status = GameStatus.DRAW;
        this.winner = null;
        this.turnStartedAt = null;
    }

    protected void notifyBoardChanged() {
        // Aktualizujemy persistent reprezentację JSONB.
        this.boardJson = toJson(boardCells);
    }

    protected void notifyMovesChanged() {
        this.movesXJson = toJsonIntList(movesX);
        this.movesOJson = toJsonIntList(movesO);
    }

    public abstract void executeMove(String playerSymbol, int position);

    public void startGame(Instant now) {
        this.status = GameStatus.IN_PROGRESS;
        this.winner = null;
        this.currentTurn = "X";
        this.turnStartedAt = now;
        this.emptySince = null;
    }

    public void resetBoardAndMoves() {
        this.boardCells = new String[9];
        this.boardJson = toJson(boardCells);

        this.movesX = new ArrayList<>(3);
        this.movesO = new ArrayList<>(3);
        this.movesXJson = toJsonIntList(movesX);
        this.movesOJson = toJsonIntList(movesO);
    }

    public void resetToWaitingForOpponent(Instant now) {
        this.status = GameStatus.WAITING_FOR_OPPONENT;
        this.winner = null;
        this.currentTurn = "X";
        this.turnStartedAt = null;
        resetBoardAndMoves();
        this.emptySince = (this.playerX == null && this.playerO == null) ? now : null;
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
    }

    public enum GameStatus {
        WAITING_FOR_OPPONENT,
        IN_PROGRESS,
        FINISHED,
        DRAW
    }

    private static String toJson(String[] cells) {
        try {
            return OBJECT_MAPPER.writeValueAsString(cells);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Nie udało się zamienić planszy na JSON", e);
        }
    }

    private static String[] fromJson(String json) {
        if (json == null) return new String[9];
        try {
            String[] cells = OBJECT_MAPPER.readValue(json, String[].class);
            if (cells.length != 9) {
                // Bezpieczeństwo, jeśli kiedyś zapis poszedł w innym formacie.
                String[] normalized = new String[9];
                System.arraycopy(cells, 0, normalized, 0, Math.min(cells.length, 9));
                return normalized;
            }
            return cells;
        } catch (Exception e) {
            throw new IllegalArgumentException("Nie udało się zamienić JSON na planszę", e);
        }
    }

    private static String toJsonIntList(List<Integer> cells) {
        try {
            return OBJECT_MAPPER.writeValueAsString(cells);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Nie udało się zamienić listy ruchów na JSON", e);
        }
    }

    private static List<Integer> fromJsonIntList(String json) {
        if (json == null) return Collections.emptyList();
        try {
            return OBJECT_MAPPER.readValue(
                    json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Integer.class)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Nie udało się zamienić JSON na listę ruchów", e);
        }
    }
}

