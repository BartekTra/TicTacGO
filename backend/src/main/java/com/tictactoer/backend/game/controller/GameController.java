package com.tictactoer.backend.game.controller;
import com.tictactoer.backend.game.controller.dto.MoveDTO;
import com.tictactoer.backend.game.service.GameService;
import com.tictactoer.backend.game.persistence.GameEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game.move")
    public void makeMove(@Payload @Valid MoveDTO moveRequest, Principal principal) {

        try {
            String currentPlayer = principal.getName();

            GameEntity updatedGame = gameService.processMove(
                    moveRequest.gameId(),
                    currentPlayer,
                    moveRequest.position()
            );

            messagingTemplate.convertAndSend("/topic/game." + moveRequest.gameId(), updatedGame);

        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    e.getMessage()
            );
        }
    }
}