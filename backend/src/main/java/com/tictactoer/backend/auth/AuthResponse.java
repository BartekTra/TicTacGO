package com.tictactoer.backend.auth;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.Formula;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String email,
        String username,
        int gamesPlayed,
        int gamesWon,
        int winRate,
        String token
        ) {}
