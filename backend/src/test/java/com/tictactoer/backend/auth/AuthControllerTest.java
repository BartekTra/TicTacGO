package com.tictactoer.backend.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final AuthRequest request = new AuthRequest("player@test.com", "PlayerX", "password123");
    private final AuthResponse responseDto = new AuthResponse(UUID.randomUUID(), "player@test.com", "PlayerX", 0, 0, 0, "mock.jwt.token");
    private final AuthResult authResult = new AuthResult("mock.jwt.token", responseDto);

    @Test
    void shouldReturnOkWithJwtHeader_whenUserRegistersSuccessfully() {
        // given
        given(authService.register(request)).willReturn(authResult);

        // when
        ResponseEntity<AuthResponse> response = authController.register(request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer mock.jwt.token");
        assertThat(response.getBody()).isEqualTo(responseDto);
        verify(authService).register(request);
    }

    @Test
    void shouldReturnOkWithJwtHeader_whenUserLogsInSuccessfully() {
        // given
        given(authService.login(request)).willReturn(authResult);

        // when
        ResponseEntity<AuthResponse> response = authController.login(request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer mock.jwt.token");
        assertThat(response.getBody()).isEqualTo(responseDto);
        verify(authService).login(request);
    }
}
