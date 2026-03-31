package com.tictactoer.backend.game.controller;
import com.tictactoer.backend.game.controller.dto.MoveDTO;
import com.tictactoer.backend.game.controller.dto.GameResponseDTO;
import com.tictactoer.backend.game.controller.dto.GameErrorDTO;
import com.tictactoer.backend.game.service.GameService;
import com.tictactoer.backend.game.persistence.GameEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game.move")
    public void makeMove(@Payload @Valid MoveDTO moveRequest, Principal principal) {

        try {
            String currentPlayer = principal != null ? principal.getName() : "UNAUTHENTICATED";
            log.info("Player {} making move in game {} at position {}", currentPlayer, moveRequest.gameId(), moveRequest.position());

            GameEntity updatedGame = gameService.processMove(
                    moveRequest.gameId(),
                    currentPlayer,
                    moveRequest.position()
            );

            messagingTemplate.convertAndSend("/topic/game." + moveRequest.gameId(), GameResponseDTO.fromEntity(updatedGame));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid move attempted by player {}: {}", principal != null ? principal.getName() : "UNAUTHENTICATED", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    principal != null ? principal.getName() : "UNAUTHENTICATED",
                    "/queue/errors",
                    new GameErrorDTO("ERROR_INVALID_MOVE", e.getMessage())
            );
        } catch (Exception e) {
            log.error("Exception during move processing for player {} in game {}: ", principal != null ? principal.getName() : "UNAUTHENTICATED", moveRequest.gameId(), e);
            messagingTemplate.convertAndSendToUser(
                    principal != null ? principal.getName() : "UNAUTHENTICATED",
                    "/queue/errors",
                    new GameErrorDTO("ERROR_INTERNAL", "Wystąpił nieoczekiwany błąd. Spróbuj ponownie.")
            );
        }
    }
}