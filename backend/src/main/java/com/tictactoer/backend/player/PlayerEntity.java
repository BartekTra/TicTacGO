package com.tictactoer.backend.player;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "players")
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private int gamesPlayed = 0;

    @Column(nullable = false)
    private int gamesWon = 0;

    protected PlayerEntity() {}

    public PlayerEntity(String username, String password, String email) {
        this.username = username;
        this.email = email;
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
}