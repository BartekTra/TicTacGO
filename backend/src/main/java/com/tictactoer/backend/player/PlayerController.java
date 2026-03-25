package com.tictactoer.backend.player;

import com.tictactoer.backend.player.dto.PlayerProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @GetMapping("/me")
    public PlayerProfileDTO getMyProfile(Principal principal) {
        if (principal == null) {
            throw new SecurityException("Musisz być zalogowany!");
        }

        return playerService.getProfile(principal.getName());
    }
}