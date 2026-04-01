package com.tictactoer.backend.oauth2;

import com.tictactoer.backend.player.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerOAuthAccountRepository extends JpaRepository<PlayerOAuthAccount, UUID> {

    Optional<PlayerOAuthAccount> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}
