package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.ClassicGameEntity;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.persistence.InfiniteGameEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameEngineTest {

    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
    }

    @Test
    void shouldThrowIllegalArgumentException_whenPositionIsOutOfBounds() {
        // given
        GameEntity classicGame = new ClassicGameEntity("X", "O");

        // when & then
        assertThatThrownBy(() -> gameEngine.executeMove(classicGame, "X", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nieprawidłowa pozycja!");

        assertThatThrownBy(() -> gameEngine.executeMove(classicGame, "O", 9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nieprawidłowa pozycja!");
    }

    @Test
    void shouldExecuteClassicMoveSuccessfully_whenPositionIsFree() {
        // given
        GameEntity classicGame = new ClassicGameEntity("X", "O");

        // when
        gameEngine.executeMove(classicGame, "X", 4);

        // then
        assertThat(classicGame.getBoard().charAt(4)).isEqualTo('X');
    }

    @Test
    void shouldThrowIllegalStateException_whenClassicSpotIsAlreadyTaken() {
        // given
        GameEntity classicGame = new ClassicGameEntity("X", "O");
        classicGame.setBoard("----X----");

        // when & then
        assertThatThrownBy(() -> gameEngine.executeMove(classicGame, "O", 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("To pole jest już zajęte!");
    }

    @Test
    void shouldExecuteInfiniteMoveSuccessfully_andQueueMove() {
        // given
        GameEntity infiniteGame = new InfiniteGameEntity("X", "O");

        // when
        gameEngine.executeMove(infiniteGame, "X", 0);

        // then
        assertThat(infiniteGame.getBoard().charAt(0)).isEqualTo('X');
        assertThat(infiniteGame.getMovesX()).isEqualTo("0");
    }

    @Test
    void shouldOverwriteOldestMove_andFreePosition_inInfiniteGame() {
        // given
        GameEntity infiniteGame = new InfiniteGameEntity("X", "O");
        infiniteGame.setBoard("XXX------");
        infiniteGame.setMovesX("0,1,2");

        gameEngine.executeMove(infiniteGame, "X", 3);

        // then
        assertThat(infiniteGame.getBoard().charAt(0)).isEqualTo('-'); 
        assertThat(infiniteGame.getBoard().charAt(3)).isEqualTo('X'); 
        assertThat(infiniteGame.getMovesX()).isEqualTo("1,2,3"); 
    }

    @Test
    void shouldAllowPlacingOverOwnOldestMove_inInfiniteGame() {
        // given
        GameEntity infiniteGame = new InfiniteGameEntity("X", "O");
        infiniteGame.setBoard("XXX------"); 
        infiniteGame.setMovesX("0,1,2");

        gameEngine.executeMove(infiniteGame, "X", 0);

        // then
        assertThat(infiniteGame.getBoard().charAt(0)).isEqualTo('X');
        assertThat(infiniteGame.getMovesX()).isEqualTo("1,2,0");
    }

    @Test
    void shouldThrowIllegalStateException_whenInfiniteSpotIsTakenByOpponentOrNewerMove() {
        // given
        GameEntity infiniteGame = new InfiniteGameEntity("X", "O");
        infiniteGame.setBoard("--O-X----");
        infiniteGame.setMovesX("4");
        infiniteGame.setMovesO("2");

        // when & then
        assertThatThrownBy(() -> gameEngine.executeMove(infiniteGame, "X", 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("To pole jest już zajęte!");
                
        assertThatThrownBy(() -> gameEngine.executeMove(infiniteGame, "X", 4))
                .isInstanceOf(IllegalStateException.class);
    }
}
