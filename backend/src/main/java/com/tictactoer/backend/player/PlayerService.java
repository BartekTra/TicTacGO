package com.tictactoer.backend.player;

import com.tictactoer.backend.player.dto.PlayerProfileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Transactional(readOnly = true)
    public PlayerProfileDTO getProfile(String email) {
        log.info("Fetching profile for player: {}", email);
        PlayerEntity player = playerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Profile fetch failed: Player {} not found", email);
                    return new IllegalArgumentException("Gracz nie istnieje!");
                });

        return PlayerProfileDTO.fromEntity(player);
    }

    @Transactional
    public void updateStatsAfterGame(String email, boolean isWinner) {
        log.info("Updating stats for player {}, winner: {}", email, isWinner);
        PlayerEntity player = playerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Stat update failed: Player {} not found", email);
                    return new IllegalArgumentException("Nie można zaktualizować statystyk. Gracz " + email + " nie istnieje!");
                });

        if (isWinner) {
            player.recordWin();
        } else {
            player.recordLossOrDraw();
        }

        playerRepository.save(player);
    }
}