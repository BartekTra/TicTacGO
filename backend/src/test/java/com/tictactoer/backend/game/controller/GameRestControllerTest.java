package com.tictactoer.backend.game.controller;

import com.tictactoer.backend.game.controller.dto.GameResponseDTO;
import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.ClassicGameEntity;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameRestControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameRestController gameRestController;

    private Principal principal;
    private GameEntity waitingGame;
    private GameEntity inProgressGame;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        
        waitingGame = new ClassicGameEntity("player@test.com", null);
        waitingGame.resetToWaitingForOpponent(java.time.Instant.now());

        inProgressGame = new ClassicGameEntity("player@test.com", "opponent@test.com");
        inProgressGame.startGame(java.time.Instant.now());
    }

    @Test
    void shouldThrowSecurityExceptionOnJoin_whenPrincipalIsNull() {
        // given
        Principal nullPrincipal = null;

        // when / then
        assertThatThrownBy(() -> gameRestController.joinGame(nullPrincipal, GameMode.CLASSIC))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Musisz być zalogowany!");
    }

    @Test
    void shouldReturnWaitingGameResponseWithoutBroadcasting_whenGameEndsUpWaiting() {
        // given
        given(principal.getName()).willReturn("player@test.com");
        given(gameService.joinOrCreateGame("player@test.com", GameMode.CLASSIC)).willReturn(waitingGame);

        // when
        ResponseEntity<GameResponseDTO> response = gameRestController.joinGame(principal, GameMode.CLASSIC);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(GameEntity.GameStatus.WAITING_FOR_OPPONENT.name());
        
        // Topic broadcast should only happen if game is IN_PROGRESS
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(GameResponseDTO.class));
    }

    @Test
    void shouldReturnInProgressGameResponseAndBroadcastToTopic_whenGameEndsUpInProgress() {
        // given
        given(principal.getName()).willReturn("player@test.com");
        given(gameService.joinOrCreateGame("player@test.com", GameMode.CLASSIC)).willReturn(inProgressGame);

        // when
        ResponseEntity<GameResponseDTO> response = gameRestController.joinGame(principal, GameMode.CLASSIC);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(GameEntity.GameStatus.IN_PROGRESS.name());

        verify(messagingTemplate).convertAndSend(
                eq("/topic/game." + inProgressGame.getGameId()),
                any(GameResponseDTO.class)
        );
    }

    @Test
    void shouldThrowSecurityExceptionOnLeave_whenPrincipalIsNull() {
        // given
        Principal nullPrincipal = null;

        // when / then
        assertThatThrownBy(() -> gameRestController.leaveGame(nullPrincipal, "game123"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Musisz być zalogowany!");
    }

    @Test
    void shouldReturnLeftGameResponseAndBroadcastToTopic_whenGameIsLeft() {
        // given
        given(principal.getName()).willReturn("player@test.com");
        given(gameService.leaveGame("game123", "player@test.com")).willReturn(waitingGame); // e.g. resets down to waiting status

        // when
        ResponseEntity<GameResponseDTO> response = gameRestController.leaveGame(principal, "game123");

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(GameEntity.GameStatus.WAITING_FOR_OPPONENT.name());

        // We always immediately broadcast left events so UI elements catch it
        verify(messagingTemplate).convertAndSend(
                eq("/topic/game." + waitingGame.getGameId()),
                any(GameResponseDTO.class)
        );
    }
}
