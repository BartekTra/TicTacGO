package com.tictactoer.backend.common;

import com.tictactoer.backend.auth.exception.InvalidCredentialsException;
import com.tictactoer.backend.auth.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturnUnauthorized_whenInvalidCredentialsExceptionThrown() {
        // given
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid auth context");

        // when
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleInvalidCredentialsException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Invalid auth context");
    }

    @Test
    void shouldReturnConflict_whenUserAlreadyExistsExceptionThrown() {
        // given
        UserAlreadyExistsException ex = new UserAlreadyExistsException("Overlap found");

        // when
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleUserAlreadyExistsException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Overlap found");
    }

    @Test
    void shouldReturnBadRequest_whenIllegalArgumentExceptionThrown() {
        // given
        IllegalArgumentException ex = new IllegalArgumentException("Bad input given");

        // when
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleIllegalArgumentException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Bad input given");
    }

    @Test
    void shouldReturnBadRequestWithMappedErrors_whenMethodArgumentNotValidExceptionThrown() {
        // given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("objectName", "email", "Must be a well-formed email address");
        FieldError fieldError2 = new FieldError("objectName", "password", "Password cannot be empty");

        given(ex.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getFieldErrors()).willReturn(List.of(fieldError1, fieldError2));

        // when
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("email", "Must be a well-formed email address");
        assertThat(response.getBody()).containsEntry("password", "Password cannot be empty");
    }

    @Test
    void shouldReturnInternalServerErrorWithSafeMessage_whenGenericExceptionThrown() {
        // given
        Exception ex = new Exception("Database schema crashed explicitly");

        // when
        ResponseEntity<Map<String, String>> response = exceptionHandler.handleGeneralException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        // Verifies the obfuscation logic is applied rather than ex.getMessage()
        assertThat(response.getBody().get("message")).isEqualTo("Wystąpił nieoczekiwany błąd serwera.");
    }
}
