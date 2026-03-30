package com.tictactoer.backend.auth;

import com.tictactoer.backend.auth.exception.InvalidCredentialsException;
import com.tictactoer.backend.auth.exception.UserAlreadyExistsException;
import com.tictactoer.backend.player.PlayerEntity;
import com.tictactoer.backend.player.PlayerRepository;
import com.tictactoer.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResult register(AuthRequest request) {
        if (playerRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Nazwa użytkownika jest już zajęta!");
        }

        if (playerRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email jest już zajęty!");
        }

        PlayerEntity newPlayer = new PlayerEntity(
                request.email(),
                request.username(),
                passwordEncoder.encode(request.password())
        );
        playerRepository.save(newPlayer);

        return buildAuthResult(newPlayer);
    }

    public AuthResult login(AuthRequest request) {
        PlayerEntity player = playerRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Nieprawidłowy login lub hasło!"));

        if (!passwordEncoder.matches(request.password(), player.getPassword())) {
            throw new InvalidCredentialsException("Nieprawidłowy login lub hasło!");
        }

        return buildAuthResult(player);
    }

    private AuthResult buildAuthResult(PlayerEntity player) {
        String token = jwtService.generateToken(player.getEmail());
        AuthResponse response = new AuthResponse(
                player.getId(), player.getEmail(), player.getUsername(),
                player.getGamesPlayed(), player.getGamesWon(), player.getWinRate()
        );
        return new AuthResult(token, response);
    }
}