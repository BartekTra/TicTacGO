package com.tictactoer.backend.auth;

import com.tictactoer.backend.player.PlayerEntity;
import com.tictactoer.backend.player.PlayerRepository;
import com.tictactoer.backend.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid AuthRequest request) {
        if (playerRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Nazwa użytkownika jest już zajęta!");
        }

        if (playerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email jest już zajęty!");
        }

        PlayerEntity newPlayer = new PlayerEntity(
                request.email(),
                request.username(),
                passwordEncoder.encode(request.password())
        );
        playerRepository.save(newPlayer);

        return getAuthResponseResponseEntity(newPlayer);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        PlayerEntity player = playerRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy login lub hasło!"));

        if (!passwordEncoder.matches(request.password(), player.getPassword())) {
            throw new IllegalArgumentException("Nieprawidłowy login lub hasło!");
        }

        return getAuthResponseResponseEntity(player);
    }

    @NonNull
    private ResponseEntity<AuthResponse> getAuthResponseResponseEntity(PlayerEntity player) {
        String token = jwtService.generateToken(player.getEmail());

        ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new AuthResponse(
                        player.getId(),
                        player.getEmail(),
                        player.getUsername(),
                        player.getGamesPlayed(),
                        player.getGamesWon(),
                        player.getWinRate()

                ));
    }
}