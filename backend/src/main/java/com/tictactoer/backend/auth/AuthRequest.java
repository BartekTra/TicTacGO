package com.tictactoer.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        String username,

        @NotBlank(message = "Email jest wymagany")
        String email,

        @NotBlank(message = "Hasło jest wymagane")
        String password
) {}