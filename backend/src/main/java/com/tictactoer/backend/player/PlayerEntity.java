package com.tictactoer.backend.player;

import com.tictactoer.backend.oauth2.PlayerOAuthAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
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

    @Column(nullable = true, length = 255)
    private String password;

    @Column(nullable = false)
    private int gamesPlayed = 0;

    @Column(nullable = false)
    private int gamesWon = 0;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PlayerOAuthAccount> oauthAccounts = new ArrayList<>();

    @Transient
    private int winRate;

    /** Constructor for classic (email + password) registration */
    public PlayerEntity(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
    }

    /** Constructor for OAuth registration (no password) */
    public PlayerEntity(String email, String username) {
        this.email = email;
        this.username = username;
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
    public List<PlayerOAuthAccount> getOauthAccounts() { return oauthAccounts; }
    public int getWinRate() {
        if (gamesPlayed == 0) return 0;
        return (int) Math.ceil(((double) gamesWon * 100) / gamesPlayed);
    }
}