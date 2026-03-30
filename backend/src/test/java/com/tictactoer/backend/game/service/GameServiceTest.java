package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.ClassicGameEntity;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.persistence.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameMatchmakingService matchmakingService;
    
    @Mock
    private GameEngine gameEngine;
    
    @Mock
    private GameRules gameRules;
    
    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private GameTimeoutScheduler gameTimeoutScheduler;

    @InjectMocks
    private GameService gameService;

    private GameEntity activeGame;
    private final String gameId = "game-123";
    private final String playerX = "playerX@test.com";
    private final String playerO = "playerO@test.com";

    @BeforeEach
    void setUp() {
        activeGame = new ClassicGameEntity(playerX, playerO);
        activeGame.startGame(java.time.Instant.now());
        // For testing purposes, we hardcode the game id reflection or assume it sets up internally.
        // It's assigned via UUID in constructor, but tests don't strictly require asserting its exact value
        // unless explicitly testing findById, in which case we mock the query lookup.
    }

    @Test
    void shouldJoinOrCreateGame_viaMatchmakingDelegate() {
        // given
        given(matchmakingService.joinOrCreateGame(playerX, GameMode.CLASSIC)).willReturn(activeGame);

        // when
        GameEntity game = gameService.joinOrCreateGame(playerX, GameMode.CLASSIC);

        // then
        assertThat(game).isEqualTo(activeGame);
        verify(matchmakingService).joinOrCreateGame(playerX, GameMode.CLASSIC);
    }

    @Test
    void shouldThrowIllegalArgumentException_whenGameNotFoundForMove() {
        // given
        given(gameRepository.findById(gameId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> gameService.processMove(gameId, playerX, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gra nie istnieje!");
    }

    @Test
    void shouldThrowIllegalStateException_whenGameIsNotInProgress() {
        // given
        activeGame.resetToWaitingForOpponent(java.time.Instant.now());
        given(gameRepository.findById(gameId)).willReturn(Optional.of(activeGame));

        // when & then
        assertThatThrownBy(() -> gameService.processMove(gameId, playerX, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gra nie jest w toku!");
    }

    @Test
    void shouldThrowIllegalStateException_whenWrongPlayerAttemptsMove() {
        // given
        given(gameRepository.findById(gameId)).willReturn(Optional.of(activeGame));
        // Current turn is X automatically.

        // when & then
        assertThatThrownBy(() -> gameService.processMove(gameId, playerO, 0)) // O tries to move on X's turn
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("To nie jest Twój ruch!");
    }

    @Test
    void shouldThrowIllegalStateException_whenUnregisteredPlayerAttemptsMove() {
        // given
        given(gameRepository.findById(gameId)).willReturn(Optional.of(activeGame));

        // when & then
        assertThatThrownBy(() -> gameService.processMove(gameId, "stranger@test.com", 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nie jesteś graczem w tej grze!");
    }

    @Test
    void shouldProcessMoveSuccessfully_andSwitchTurns() {
        // given
        given(gameRepository.findById(gameId)).willReturn(Optional.of(activeGame));
        given(gameRules.hasWon(activeGame.getBoard(), "X")).willReturn(false);
        given(gameRules.isBoardFull(activeGame.getBoard())).willReturn(false);

        // when
        GameEntity result = gameService.processMove(gameId, playerX, 4);

        // then
        verify(gameEngine).executeMove(activeGame, "X", 4);
        assertThat(result.getCurrentTurn()).isEqualTo("O"); // Switched turn
        verify(gameRepository).save(activeGame);
        verify(gameTimeoutScheduler).scheduleTurnInactivity(any(), any(), eq("O"));
    }

    @Test
    void shouldMarkFinished_whenPlayerWinsWithMove() {
        // given
        given(gameRepository.findById(gameId)).willReturn(Optional.of(activeGame));
        given(gameRules.hasWon(anyString(), eq("X"))).willReturn(true); // Game engine intercepts the board directly

        // when
        GameEntity result = gameService.processMove(gameId, playerX, 0);

        // then
        verify(gameEngine).executeMove(activeGame, "X", 0);
        assertThat(result.getStatus()).isEqualTo(GameEntity.GameStatus.FINISHED);
        assertThat(result.getWinner()).isEqualTo(playerX);
        verify(gameRepository).save(activeGame);
        verify(gameTimeoutScheduler, never()).scheduleTurnInactivity(any(), any(), any());
    }

    @Test
    void shouldMarkDraw_whenBoardFillsUpInClassicMode() {
        // given
        activeGame.setMode(GameMode.CLASSIC);
        given(gameRepository.findById(gameId)).willReturn(Optional.of(activeGame));
        given(gameRules.hasWon(anyString(), eq("X"))).willReturn(false);
        given(gameRules.isBoardFull(anyString())).willReturn(true);

        // when
        GameEntity result = gameService.processMove(gameId, playerX, 8);

        // then
        assertThat(result.getStatus()).isEqualTo(GameEntity.GameStatus.DRAW);
        assertThat(result.getWinner()).isNull();
        verify(gameRepository).save(activeGame);
    }

    @Test
    void shouldThrowGameConcurrencyConflictException_whenOptimisticLockFailsOnMove() {
        // given
        given(gameRepository.findById(gameId)).willReturn(Optional.of(activeGame));
        given(gameRepository.save(activeGame)).willThrow(new ObjectOptimisticLockingFailureException(GameEntity.class, "optimistic lock"));

        // when & then
        assertThatThrownBy(() -> gameService.processMove(gameId, playerX, 0))
                .isInstanceOf(GameConcurrencyConflictException.class)
                .hasMessageContaining("Konflikt współbieżności podczas wykonywania ruchu. Spróbuj ponownie.");
    }

    @Test
    void shouldLeaveGame_andRescheduleEmptyCleanup() {
        // given
        given(gameRepository.findById(gameId)).willReturn(Optional.of(activeGame));

        // when P1 leaves
        GameEntity result = gameService.leaveGame(gameId, playerX);

        // then P1 is stripped, game resets to waiting, scheduler is NOT called empty yet
        assertThat(result.getPlayerX()).isNull();
        assertThat(result.getStatus()).isEqualTo(GameEntity.GameStatus.WAITING_FOR_OPPONENT);
        verify(gameTimeoutScheduler, never()).scheduleEmptyDeletion(any(), any());

        // when P2 leaves
        gameService.leaveGame(gameId, playerO);
        
        // then
        assertThat(result.getPlayerO()).isNull();
        verify(gameTimeoutScheduler).scheduleEmptyDeletion(eq(result.getGameId()), any());
    }
}
