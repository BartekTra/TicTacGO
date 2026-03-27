package com.tictactoer.backend.player.dto;

import com.tictactoer.backend.player.PlayerEntity;

public record PlayerProfileDTO(
        String username,
        String email,
        int gamesPlayed,
        int gamesWon,
        int winRate
) {
    public static PlayerProfileDTO fromEntity(PlayerEntity entity) {
        int rate = entity.getGamesPlayed() == 0
                ? 0
                : (int) Math.round((double) entity.getGamesWon() / entity.getGamesPlayed() * 100.0);

        return new PlayerProfileDTO(
                entity.getUsername(),
                entity.getEmail(),
                entity.getGamesPlayed(),
                entity.getGamesWon(),
                rate
        );
    }
}