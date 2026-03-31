package com.tictactoer.backend.game.service;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.ClassicGameEntity;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.persistence.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.ConcurrencyFailureException;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameMatchmakingServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameTimeoutScheduler gameTimeoutScheduler;

    private GameMatchmakingService gameMatchmakingService;
    private GameMatchmakingService selfProxyMock;

    @BeforeEach
    void setUp() throws Exception {
        gameMatchmakingService = new GameMatchmakingService(gameRepository, gameTimeoutScheduler);

        selfProxyMock = Mockito.mock(GameMatchmakingService.class);
        Field selfField = GameMatchmakingService.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(gameMatchmakingService, selfProxyMock);
    }

    @Test
    void shouldRetryAndSucceed_whenConcurrencyExceptionOccursInFirstAttempt() {
        // given
        String playerEmail = "player@test.com";
        GameMode mode = GameMode.CLASSIC;
        GameEntity mockGame = new ClassicGameEntity(playerEmail, "opponent@test.com");

        given(selfProxyMock.joinOrCreateGameAttempt(playerEmail, mode))
                .willThrow(new ConcurrencyFailureException("Lock failed"))
                .willReturn(mockGame);

        // when
        GameEntity result = gameMatchmakingService.joinOrCreateGame(playerEmail, mode);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockGame);
        verify(selfProxyMock, times(2)).joinOrCreateGameAttempt(playerEmail, mode);
    }

    @Test
    void shouldThrowGameConflictException_whenMaxRetriesExceeded() {
        // given
        String playerEmail = "player@test.com";
        GameMode mode = GameMode.CLASSIC;

        given(selfProxyMock.joinOrCreateGameAttempt(playerEmail, mode))
                .willThrow(new ConcurrencyFailureException("Lock failed constantly"));

        // when / then
        assertThatThrownBy(() -> gameMatchmakingService.joinOrCreateGame(playerEmail, mode))
                .isInstanceOf(GameConcurrencyConflictException.class)
                .hasMessageContaining("Konflikt współbieżności podczas dołączania do gry. Spróbuj ponownie.");

        verify(selfProxyMock, times(5)).joinOrCreateGameAttempt(playerEmail, mode);
    }

    @Test
    void shouldJoinExistingGameAsO_whenGameWaitingWithX() {
        String playerX = "playerX@test.com";
        String playerO = "playerO@test.com";
        GameMode mode = GameMode.CLASSIC;

        GameEntity waitingGame = new ClassicGameEntity(playerX, null);
        
        given(gameRepository.findFirstByStatusAndModeAndPlayerOIsNullAndPlayerXIsNotNull(
                GameEntity.GameStatus.WAITING_FOR_OPPONENT, mode))
                .willReturn(Optional.of(waitingGame));

        // when
        GameEntity result = gameMatchmakingService.joinOrCreateGameAttempt(playerO, mode);

        // then
        assertThat(result.getPlayerO()).isEqualTo(playerO);
        assertThat(result.getPlayerX()).isEqualTo(playerX);
        assertThat(result.getStatus()).isEqualTo(GameEntity.GameStatus.IN_PROGRESS);
        
        verify(gameRepository).save(waitingGame);
        verify(gameRepository).flush();
        verify(gameTimeoutScheduler).scheduleTurnInactivity(eq(waitingGame.getGameId()), any(), eq("X"));
    }

    @Test
    void shouldJoinExistingGameAsX_whenGameWaitingWithO() {
        // given
        String playerX = "playerX@test.com";
        String playerO = "playerO@test.com";
        GameMode mode = GameMode.INFINITE;

        GameEntity waitingGame = new ClassicGameEntity(null, playerO);
        waitingGame.setMode(mode);

        given(gameRepository.findFirstByStatusAndModeAndPlayerOIsNullAndPlayerXIsNotNull(
                GameEntity.GameStatus.WAITING_FOR_OPPONENT, mode))
                .willReturn(Optional.empty());

        given(gameRepository.findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNotNull(
                GameEntity.GameStatus.WAITING_FOR_OPPONENT, mode))
                .willReturn(Optional.of(waitingGame));

        // when
        GameEntity result = gameMatchmakingService.joinOrCreateGameAttempt(playerX, mode);

        // then
        assertThat(result.getPlayerX()).isEqualTo(playerX);
        assertThat(result.getPlayerO()).isEqualTo(playerO);
        assertThat(result.getStatus()).isEqualTo(GameEntity.GameStatus.IN_PROGRESS);

        verify(gameRepository).save(waitingGame);
        verify(gameRepository).flush();
        verify(gameTimeoutScheduler).scheduleTurnInactivity(eq(waitingGame.getGameId()), any(), eq("X"));
    }

    @Test
    void shouldJoinEmptyGame_whenNoPartiallyFilledGamesFound() {
        // given
        String player = "player@test.com";
        GameMode mode = GameMode.CLASSIC;

        GameEntity emptyGame = new ClassicGameEntity(null, null);

        given(gameRepository.findFirstByStatusAndModeAndPlayerOIsNullAndPlayerXIsNotNull(
                GameEntity.GameStatus.WAITING_FOR_OPPONENT, mode))
                .willReturn(Optional.empty());

        given(gameRepository.findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNotNull(
                GameEntity.GameStatus.WAITING_FOR_OPPONENT, mode))
                .willReturn(Optional.empty());

        given(gameRepository.findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNullAndEmptySinceAfter(
                eq(GameEntity.GameStatus.WAITING_FOR_OPPONENT), eq(mode), any()))
                .willReturn(Optional.of(emptyGame));

        // when
        GameEntity result = gameMatchmakingService.joinOrCreateGameAttempt(player, mode);

        // then
        assertThat(result.getStatus()).isEqualTo(GameEntity.GameStatus.WAITING_FOR_OPPONENT);
        
        boolean playerGotX = player.equals(result.getPlayerX());
        boolean playerGotO = player.equals(result.getPlayerO());
        assertThat(playerGotX ^ playerGotO).isTrue();
        
        verify(gameRepository).save(emptyGame);
        verify(gameRepository).flush();
    }

    @Test
    void shouldCreateNewGame_whenNoAvailableGamesFound() {
        // given
        String player = "player@test.com";
        GameMode mode = GameMode.INFINITE;

        given(gameRepository.findFirstByStatusAndModeAndPlayerOIsNullAndPlayerXIsNotNull(
                GameEntity.GameStatus.WAITING_FOR_OPPONENT, mode))
                .willReturn(Optional.empty());

        given(gameRepository.findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNotNull(
                GameEntity.GameStatus.WAITING_FOR_OPPONENT, mode))
                .willReturn(Optional.empty());

        given(gameRepository.findFirstByStatusAndModeAndPlayerXIsNullAndPlayerOIsNullAndEmptySinceAfter(
                eq(GameEntity.GameStatus.WAITING_FOR_OPPONENT), eq(mode), any()))
                .willReturn(Optional.empty());

        // when
        GameEntity result = gameMatchmakingService.joinOrCreateGameAttempt(player, mode);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMode()).isEqualTo(GameMode.INFINITE);
        assertThat(result.getStatus()).isEqualTo(GameEntity.GameStatus.WAITING_FOR_OPPONENT);

        boolean playerGotX = player.equals(result.getPlayerX());
        boolean playerGotO = player.equals(result.getPlayerO());
        assertThat(playerGotX ^ playerGotO).isTrue();

        verify(gameRepository).save(any(GameEntity.class));
        verify(gameRepository).flush();
    }
}
