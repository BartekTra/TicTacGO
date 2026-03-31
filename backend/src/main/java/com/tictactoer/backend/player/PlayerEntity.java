package com.tictactoer.backend.player;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "players")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false)
    private int gamesPlayed = 0;

    @Column(nullable = false)
    private int gamesWon = 0;

    @Transient
    private int winRate;

    public PlayerEntity(String email, String username, String password ) {
        this.email = email;
        this.username = username;
        this.password = password;
    }

    public void recordWin() {
        this.gamesPlayed++;
        this.gamesWon++;
    }

    public void recordLossOrDraw() {
        this.gamesPlayed++;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public int getGamesPlayed() { return gamesPlayed; }
    public int getGamesWon() { return gamesWon; }
    public String getPassword() { return password; }
    public int getWinRate() {
        if (gamesPlayed == 0) return 0;
        return (int) Math.ceil(((double) gamesWon * 100) / gamesPlayed);
    }
}