package com.tictactoer.backend.game.controller;

import com.tictactoer.backend.game.controller.dto.GameErrorDTO;
import com.tictactoer.backend.game.controller.dto.GameResponseDTO;
import com.tictactoer.backend.game.controller.dto.MoveDTO;
import com.tictactoer.backend.game.persistence.ClassicGameEntity;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameController gameController;

    private Principal principal;
    private final MoveDTO validMove = new MoveDTO("game123", 4);
    private GameEntity updatedGame;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        
        updatedGame = new ClassicGameEntity("player@test.com", "opponent@test.com");
    }

    @Test
    void shouldProcessMoveSuccessfully_andBroadcastStateToGameTopic() {
        // given
        given(principal.getName()).willReturn("player@test.com");
        given(gameService.processMove("game123", "player@test.com", 4)).willReturn(updatedGame);

        // when
        gameController.makeMove(validMove, principal);

        // then
        verify(gameService).processMove("game123", "player@test.com", 4);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/game.game123"),
                any(GameResponseDTO.class)
        );
    }

    @Test
    void shouldCatchIllegalArgumentException_andSendErrorDtoToUserQueue() {
        // given
        given(principal.getName()).willReturn("player@test.com");
        IllegalArgumentException expectedException = new IllegalArgumentException("Nieprawidłowa pozycja!");
        given(gameService.processMove("game123", "player@test.com", 4)).willThrow(expectedException);

        // when
        gameController.makeMove(validMove, principal);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                "player@test.com",
                "/queue/errors",
                new GameErrorDTO("ERROR_INVALID_MOVE", expectedException.getMessage())
        );
    }

    @Test
    void shouldCatchIllegalStateException_andSendErrorDtoToUserQueue() {
        // given
        given(principal.getName()).willReturn("player@test.com");
        IllegalStateException expectedException = new IllegalStateException("To nie jest Twój ruch!");
        given(gameService.processMove("game123", "player@test.com", 4)).willThrow(expectedException);

        // when
        gameController.makeMove(validMove, principal);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                "player@test.com",
                "/queue/errors",
                new GameErrorDTO("ERROR_INVALID_MOVE", expectedException.getMessage())
        );
    }

    @Test
    void shouldCatchGenericException_andSendInternalErrorDtoToUserQueue() {
        // given
        given(principal.getName()).willReturn("player@test.com");
        given(gameService.processMove("game123", "player@test.com", 4))
                .willThrow(new RuntimeException("Database timeout"));

        // when
        gameController.makeMove(validMove, principal);

        // then
        verify(messagingTemplate).convertAndSendToUser(
                "player@test.com",
                "/queue/errors",
                new GameErrorDTO("ERROR_INTERNAL", "Wystąpił nieoczekiwany błąd. Spróbuj ponownie.")
        );
    }

    @Test
    void shouldProcessMoveProperly_whenPrincipalIsNull() {
        // given
        given(gameService.processMove("game123", "UNAUTHENTICATED", 4)).willReturn(updatedGame);

        // when
        gameController.makeMove(validMove, null);

        // then
        verify(gameService).processMove("game123", "UNAUTHENTICATED", 4);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/game.game123"),
                any(GameResponseDTO.class)
        );
    }
}
