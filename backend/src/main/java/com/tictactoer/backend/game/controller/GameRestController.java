package com.tictactoer.backend.game.controller;

import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.persistence.GameEntity;
import com.tictactoer.backend.game.controller.dto.GameResponseDTO;
import com.tictactoer.backend.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameRestController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/join")
    public ResponseEntity<GameResponseDTO> joinGame(
            Principal principal,
            @RequestParam(defaultValue = "CLASSIC") GameMode mode) {

        log.info("Request to join game received from user: {}, mode: {}", principal != null ? principal.getName() : "UNAUTHENTICATED", mode);
        if (principal == null) {
            log.warn("Join game failed: Unauthenticated user");
            throw new SecurityException("Musisz być zalogowany!");
        }

        GameEntity game = gameService.joinOrCreateGame(principal.getName(), mode);

        if (game.getStatus() == GameEntity.GameStatus.IN_PROGRESS) {
            messagingTemplate.convertAndSend("/topic/game." + game.getGameId(), GameResponseDTO.fromEntity(game));
        }

        return ResponseEntity.ok(GameResponseDTO.fromEntity(game));
    }

    @PostMapping("/leave")
    public ResponseEntity<GameResponseDTO> leaveGame(
            Principal principal,
            @RequestParam String gameId
    ) {
        log.info("Request to leave game {} received from user: {}", gameId, principal != null ? principal.getName() : "UNAUTHENTICATED");
        if (principal == null) {
            log.warn("Leave game failed: Unauthenticated user for game {}", gameId);
            throw new SecurityException("Musisz być zalogowany!");
        }

        GameEntity updatedGame = gameService.leaveGame(gameId, principal.getName());

        messagingTemplate.convertAndSend("/topic/game." + updatedGame.getGameId(), GameResponseDTO.fromEntity(updatedGame));

        return ResponseEntity.ok(GameResponseDTO.fromEntity(updatedGame));
    }
}