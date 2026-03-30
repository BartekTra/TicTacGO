package com.tictactoer.backend.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameRulesTest {

    private GameRules gameRules;

    @BeforeEach
    void setUp() {
        gameRules = new GameRules();
    }

    @Test
    void shouldReturnTrue_whenHorizontalWinConditionMet() {
        // given
        String boardTopRowX = "XXXO--O--";
        String boardMidRowO = "X--OOO--X";
        String boardBotRowX = "O--O--XXX";

        // when & then
        assertThat(gameRules.hasWon(boardTopRowX, "X")).isTrue();
        assertThat(gameRules.hasWon(boardMidRowO, "O")).isTrue();
        assertThat(gameRules.hasWon(boardBotRowX, "X")).isTrue();
    }

    @Test
    void shouldReturnTrue_whenVerticalWinConditionMet() {
        // given
        String boardLeftColX = "X--X--XOO";
        String boardMidColO = "-O--O--O-";
        String boardRightColX = "--X--X--X";

        // when & then
        assertThat(gameRules.hasWon(boardLeftColX, "X")).isTrue();
        assertThat(gameRules.hasWon(boardMidColO, "O")).isTrue();
        assertThat(gameRules.hasWon(boardRightColX, "X")).isTrue();
    }

    @Test
    void shouldReturnTrue_whenDiagonalWinConditionMet() {
        // given
        String boardMainDiagX = "X---X---X";
        String boardAntiDiagO = "--O-O-O--";

        // when & then
        assertThat(gameRules.hasWon(boardMainDiagX, "X")).isTrue();
        assertThat(gameRules.hasWon(boardAntiDiagO, "O")).isTrue();
    }

    @Test
    void shouldReturnFalse_whenNoWinConditionMet() {
        // given
        String boardEmpty = "---------";
        String boardMixed = "XOXOXOOXO";

        // when & then
        assertThat(gameRules.hasWon(boardEmpty, "X")).isFalse();
        assertThat(gameRules.hasWon(boardMixed, "X")).isFalse();
        assertThat(gameRules.hasWon(boardMixed, "O")).isFalse(); // Drawn game
    }

    @Test
    void shouldReturnTrue_whenBoardIsFull() {
        // given
        String fullBoard = "XOXOXOOXO";

        // when
        boolean isFull = gameRules.isBoardFull(fullBoard);

        // then
        assertThat(isFull).isTrue();
    }

    @Test
    void shouldReturnFalse_whenBoardHasEmptySpots() {
        // given
        String partiallyFilledBoard = "XO-OXO-XO";
        String emptyBoard = "---------";

        // when & then
        assertThat(gameRules.isBoardFull(partiallyFilledBoard)).isFalse();
        assertThat(gameRules.isBoardFull(emptyBoard)).isFalse();
    }
}
