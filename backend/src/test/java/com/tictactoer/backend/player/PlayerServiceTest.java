package com.tictactoer.backend.player;

import com.tictactoer.backend.player.dto.PlayerProfileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    private PlayerEntity testPlayer;
    private final String email = "player@test.com";

    @BeforeEach
    void setUp() {
        testPlayer = new PlayerEntity(email, "PlayerTest", "superSecret");
    }

    @Test
    void shouldSuccessfullyReturnPlayerProfile_whenEmailExists() {
        // given
        given(playerRepository.findByEmail(email)).willReturn(Optional.of(testPlayer));

        // when
        PlayerProfileDTO profile = playerService.getProfile(email);

        // then
        assertThat(profile).isNotNull();
        assertThat(profile.email()).isEqualTo(email);
        assertThat(profile.username()).isEqualTo("PlayerTest");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnGetProfile_whenEmailNotFound() {
        // given
        given(playerRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> playerService.getProfile("ghost@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gracz nie istnieje!");
    }

    @Test
    void shouldProperlyRecordWin_whenPlayerWins() {
        // given
        given(playerRepository.findByEmail(email)).willReturn(Optional.of(testPlayer));

        // when
        playerService.updateStatsAfterGame(email, true);

        // then
        assertThat(testPlayer.getGamesPlayed()).isEqualTo(1);
        assertThat(testPlayer.getGamesWon()).isEqualTo(1);
        verify(playerRepository).save(testPlayer);
    }

    @Test
    void shouldProperlyRecordLossOrDraw_whenPlayerDoesNotWin() {
        // given
        given(playerRepository.findByEmail(email)).willReturn(Optional.of(testPlayer));

        // when
        playerService.updateStatsAfterGame(email, false);

        // then
        assertThat(testPlayer.getGamesPlayed()).isEqualTo(1);
        assertThat(testPlayer.getGamesWon()).isEqualTo(0);
        verify(playerRepository).save(testPlayer);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnUpdateStats_whenEmailNotFound() {
        // given
        given(playerRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> playerService.updateStatsAfterGame("ghost@test.com", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nie można zaktualizować statystyk.");
    }
}
