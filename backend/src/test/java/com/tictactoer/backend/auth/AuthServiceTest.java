package com.tictactoer.backend.auth;

import com.tictactoer.backend.auth.exception.InvalidCredentialsException;
import com.tictactoer.backend.auth.exception.UserAlreadyExistsException;
import com.tictactoer.backend.player.PlayerEntity;
import com.tictactoer.backend.player.PlayerRepository;
import com.tictactoer.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private AuthRequest validRequest;
    private PlayerEntity mappedPlayer;

    @BeforeEach
    void setUp() {
        validRequest = new AuthRequest("test@test.com", "TestUser", "password123");
        mappedPlayer = new PlayerEntity("test@test.com", "TestUser", "encodedPassword123");
    }

    @Test
    void shouldRegisterSuccessfully_whenUserDoesNotExist() {
        // given
        given(playerRepository.existsByUsername("TestUser")).willReturn(false);
        given(playerRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword123");
        given(jwtService.generateToken("test@test.com")).willReturn("mock.jwt.token");

        // when
        AuthResult result = authService.register(validRequest);

        // then
        assertThat(result.token()).isEqualTo("mock.jwt.token");
        assertThat(result.response().email()).isEqualTo("test@test.com");
        assertThat(result.response().username()).isEqualTo("TestUser");
        verify(playerRepository).save(any(PlayerEntity.class));
    }

    @Test
    void shouldThrowUserAlreadyExistsException_whenRegisteringWithTakenUsername() {
        // given
        given(playerRepository.existsByUsername("TestUser")).willReturn(true);

        // when / then
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Nazwa użytkownika jest już zajęta!");
    }

    @Test
    void shouldThrowUserAlreadyExistsException_whenRegisteringWithTakenEmail() {
        // given
        given(playerRepository.existsByUsername("TestUser")).willReturn(false);
        given(playerRepository.existsByEmail("test@test.com")).willReturn(true);

        // when / then
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email jest już zajęty!");
    }

    @Test
    void shouldLoginSuccessfully_whenCredentialsAreValid() {
        // given
        given(playerRepository.findByEmail("test@test.com")).willReturn(Optional.of(mappedPlayer));
        given(passwordEncoder.matches("password123", "encodedPassword123")).willReturn(true);
        given(jwtService.generateToken("test@test.com")).willReturn("mock.jwt.token");

        // when
        AuthResult result = authService.login(validRequest);

        // then
        assertThat(result.token()).isEqualTo("mock.jwt.token");
        assertThat(result.response().username()).isEqualTo("TestUser");
        assertThat(result.response().email()).isEqualTo("test@test.com");
    }

    @Test
    void shouldThrowInvalidCredentialsException_whenLoginFailsDueToEmailNotFound() {
        // given
        given(playerRepository.findByEmail("test@test.com")).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> authService.login(validRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Nieprawidłowy login lub hasło!");
    }

    @Test
    void shouldThrowInvalidCredentialsException_whenLoginFailsDueToIncorrectPassword() {
        // given
        given(playerRepository.findByEmail("test@test.com")).willReturn(Optional.of(mappedPlayer));
        given(passwordEncoder.matches("password123", "encodedPassword123")).willReturn(false);

        // when / then
        assertThatThrownBy(() -> authService.login(validRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Nieprawidłowy login lub hasło!");
    }
}
