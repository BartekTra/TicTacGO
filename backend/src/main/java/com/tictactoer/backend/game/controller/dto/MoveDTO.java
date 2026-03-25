package com.tictactoer.backend.game.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MoveDTO(

        @NotBlank(message = "Identyfikator gry (gameId) nie może być pusty!")
        String gameId,

        @NotNull(message = "Pozycja na planszy jest wymagana!")
        @Min(value = 0, message = "Pozycja nie może być mniejsza niż 0")
        @Max(value = 8, message = "Pozycja nie może być większa niż 8")
        Integer position

) {}