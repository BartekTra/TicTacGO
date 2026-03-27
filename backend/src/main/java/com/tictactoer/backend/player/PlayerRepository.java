package com.tictactoer.backend.player;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    Optional<PlayerEntity> findByUsername(String username);
    Optional<PlayerEntity> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}