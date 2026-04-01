package com.tictactoer.backend.oauth2;

import com.tictactoer.backend.player.PlayerEntity;
import com.tictactoer.backend.player.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles the user resolution logic after a successful OAuth2 authorization.
 *
 * Account linking strategy:
 *  1. Look up PlayerOAuthAccount by (provider, providerId).
 *     → Found: return the associated PlayerEntity (returning user, same provider).
 *  2. Not found → look up PlayerEntity by email.
 *     → Found: link this provider to the existing player (user had signed up via
 *              another provider or with email/password using the same address).
 *  3. Neither found → create a new PlayerEntity + PlayerOAuthAccount (brand new user).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final PlayerRepository playerRepository;
    private final PlayerOAuthAccountRepository oauthAccountRepository;

    @Transactional
    public PlayerEntity processOAuthUser(OAuth2User oauthUser, String registrationId) {
        OAuthProvider provider = resolveProvider(registrationId);
        String providerId = extractProviderId(oauthUser, provider);
        String email = extractEmail(oauthUser, provider);
        String username = extractUsername(oauthUser, email);

        log.info("Processing OAuth login — provider={}, email={}", provider, email);

        // 1. Look up by (provider, providerId)
        Optional<PlayerOAuthAccount> existingOAuthAccount =
                oauthAccountRepository.findByProviderAndProviderId(provider, providerId);

        if (existingOAuthAccount.isPresent()) {
            log.info("Returning OAuth user found: player_id={}", existingOAuthAccount.get().getPlayer().getId());
            return existingOAuthAccount.get().getPlayer();
        }

        // 2. Look up by email (account linking)
        PlayerEntity player = playerRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 3. Brand new user — create PlayerEntity
                    String safeUsername = ensureUniqueUsername(username);
                    log.info("Creating new player via OAuth: email={}, username={}", email, safeUsername);
                    return playerRepository.save(new PlayerEntity(email, safeUsername));
                });

        // Link this OAuth account to the (new or existing) player
        PlayerOAuthAccount newOAuthAccount = new PlayerOAuthAccount(player, provider, providerId);
        oauthAccountRepository.save(newOAuthAccount);
        log.info("Linked {} account (providerId={}) to player_id={}", provider, providerId, player.getId());

        return player;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private OAuthProvider resolveProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> OAuthProvider.GOOGLE;
            case "github" -> OAuthProvider.GITHUB;
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + registrationId);
        };
    }

    private String extractProviderId(OAuth2User oauthUser, OAuthProvider provider) {
        Object id = switch (provider) {
            case GOOGLE -> oauthUser.getAttribute("sub");
            case GITHUB -> oauthUser.getAttribute("id");
        };
        if (id == null) {
            throw new IllegalStateException("Could not extract provider ID from OAuth2 user attributes");
        }
        return id.toString();
    }

    private String extractEmail(OAuth2User oauthUser, OAuthProvider provider) {
        String email = oauthUser.getAttribute("email");
        if (email == null || email.isBlank()) {
            // GitHub users with private emails may not expose their email in the main profile.
            // In that case we fall back to a synthetic email.
            String login = oauthUser.getAttribute("login");
            String providerId = extractProviderId(oauthUser, provider);
            email = (login != null ? login : providerId) + "@github-noreply.com";
            log.warn("GitHub user has no public email — using synthetic address: {}", email);
        }
        return email;
    }

    private String extractUsername(OAuth2User oauthUser, String email) {
        // Google provides "name", GitHub provides "login"
        String name = oauthUser.getAttribute("name");
        if (name == null || name.isBlank()) {
            name = oauthUser.getAttribute("login");
        }
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }
        // Strip characters not allowed in our 50-char username column
        return name.replaceAll("[^a-zA-Z0-9_\\- ]", "").trim();
    }

    /**
     * If the extracted username is already taken, appends a short random suffix.
     */
    private String ensureUniqueUsername(String preferred) {
        String candidate = preferred.length() > 45 ? preferred.substring(0, 45) : preferred;
        if (!playerRepository.existsByUsername(candidate)) {
            return candidate;
        }
        String suffix = UUID.randomUUID().toString().substring(0, 4);
        return candidate + "_" + suffix;
    }
}
