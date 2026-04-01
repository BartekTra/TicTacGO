package com.tictactoer.backend.oauth2;

import com.tictactoer.backend.player.PlayerEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Maps an OAuth provider account (Google/GitHub) to a PlayerEntity.
 * Enables account linking: one player can log in via multiple providers
 * if they share the same email address.
 */
@Entity
@Table(
    name = "player_oauth_accounts",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerOAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    /** The user's unique ID at the OAuth provider side (e.g. Google sub, GitHub id) */
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    public PlayerOAuthAccount(PlayerEntity player, OAuthProvider provider, String providerId) {
        this.player = player;
        this.provider = provider;
        this.providerId = providerId;
    }

    public UUID getId() { return id; }
    public PlayerEntity getPlayer() { return player; }
    public OAuthProvider getProvider() { return provider; }
    public String getProviderId() { return providerId; }
}
