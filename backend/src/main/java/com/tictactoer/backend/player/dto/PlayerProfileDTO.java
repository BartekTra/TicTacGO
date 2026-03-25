package com.tictactoer.backend.player.dto;

import com.tictactoer.backend.player.PlayerEntity;

public record PlayerProfileDTO(
        String username,
        int gamesPlayed,
        int gamesWon,
        double winRate
) {
    public static PlayerProfileDTO fromEntity(PlayerEntity entity) {
        double rate = entity.getGamesPlayed() == 0
                ? 0.0
                : Math.round(((double) entity.getGamesWon() / entity.getGamesPlayed()) * 100.0);

        return new PlayerProfileDTO(
                entity.getUsername(),
                entity.getGamesPlayed(),
                entity.getGamesWon(),
                rate
        );
    }
}