package com.tictactoer.backend.player;

import com.tictactoer.backend.player.dto.PlayerProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public PlayerProfileDTO getProfile(String username) {
        PlayerEntity player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Gracz nie istnieje!"));

        return PlayerProfileDTO.fromEntity(player);
    }

    @Transactional
    public void updateStatsAfterGame(String username, boolean isWinner) {
        PlayerEntity player = playerRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Nie można zaktualizować statystyk. Gracz " + username + " nie istnieje!"));

        if (isWinner) {
            player.recordWin();
        } else {
            player.recordLossOrDraw();
        }

        playerRepository.save(player);
    }
}