package com.tictactoer.backend.auth;

public record AuthResult(String token, AuthResponse response) {}