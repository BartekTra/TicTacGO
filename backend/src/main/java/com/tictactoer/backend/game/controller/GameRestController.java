package com.tictactoer.backend.game.controller;

import com.tictactoer.backend.game.domain.Game;
import com.tictactoer.backend.game.domain.GameMode;
import com.tictactoer.backend.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameRestController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/join")
    public ResponseEntity<Game> joinGame(
            Principal principal,
            @RequestParam(defaultValue = "CLASSIC") GameMode mode) {

        if (principal == null) {
            throw new SecurityException("Musisz być zalogowany!");
        }

        Game game = gameService.joinOrCreateGame(principal.getName(), mode);

        if (game.getStatus() == Game.GameStatus.IN_PROGRESS) {
            messagingTemplate.convertAndSend("/topic/game." + game.getGameId(), game);
        }

        return ResponseEntity.ok(game);
    }
}