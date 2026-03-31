package com.tictactoer.backend.player;

import com.tictactoer.backend.player.dto.PlayerProfileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlayerControllerTest {

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private PlayerController playerController;

    private Principal principal;
    private final String email = "player@test.com";
    private PlayerProfileDTO mockProfile;

    @BeforeEach
    void setUp() {
        principal = mock(Principal.class);
        mockProfile = new PlayerProfileDTO("playerX", email, 10, 5, 50);
    }

    @Test
    void shouldReturnProfileOk_whenPrincipalIsValid() {
        // given
        given(principal.getName()).willReturn(email);
        given(playerService.getProfile(email)).willReturn(mockProfile);

        // when
        PlayerProfileDTO response = playerController.getMyProfile(principal);

        // then
        assertThat(response).isNotNull();
        assertThat(response).isEqualTo(mockProfile);
        verify(playerService).getProfile(email);
    }

    @Test
    void shouldThrowSecurityException_whenPrincipalIsNull() {
        // given
        Principal nullPrincipal = null;

        // when / then
        assertThatThrownBy(() -> playerController.getMyProfile(nullPrincipal))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Musisz być zalogowany");
    }
}
