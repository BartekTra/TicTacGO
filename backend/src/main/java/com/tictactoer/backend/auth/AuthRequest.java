package com.tictactoer.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Nazwa użytkownika jest wymagana")
        String username,

        @NotBlank(message = "Email jest wymagany")
        String email,

        @NotBlank(message = "Hasło jest wymagane")
        String password
) {}