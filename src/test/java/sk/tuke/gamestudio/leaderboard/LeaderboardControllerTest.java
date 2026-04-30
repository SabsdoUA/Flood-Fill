package sk.tuke.gamestudio.leaderboard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.authentication.core.model.User;
import sk.tuke.gamestudio.authentication.core.service.UserService;
import sk.tuke.gamestudio.leaderboard.model.UserStats.BoardSize;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderboardControllerTest {

    @Mock
    private LeaderboardService leaderboardService;
    @Mock
    private UserService userService;
    @Mock
    private Principal principal;

    @InjectMocks
    private LeaderboardController leaderboardController;

    @ParameterizedTest
    @CsvSource({
            "12,SMALL",
            "15,MEDIUM",
            "18,LARGE"
    })
    void givenValidPrincipalUser_whenRecordWin_thenDelegateToService(int boardSize, BoardSize expected) {
        // Given
        User user = new User();
        when(userService.resolveUser(principal)).thenReturn(Optional.of(user));

        // When
        leaderboardController.recordWin(principal, boardSize, "game-1");

        // Then
        verify(leaderboardService).recordWin(user, "game-1", expected);
        verify(userService, never()).resolveOAuthUser();
        verify(userService, never()).currentUser();
    }

    @Test
    void givenPrincipalNotResolvedButCurrentUserResolved_whenRecordWin_thenUseFallbackContext() {
        // Given
        User user = new User();
        UserService.UserContext context = new UserService.UserContext("fallback@example.com");
        when(userService.resolveUser(principal)).thenReturn(Optional.empty());
        when(userService.resolveOAuthUser()).thenReturn("Neprihlásený používateľ");
        when(userService.currentUser()).thenReturn(context);
        when(userService.resolveUser(context)).thenReturn(Optional.of(user));

        // When
        leaderboardController.recordWin(principal, 15, "game-2");

        // Then
        verify(leaderboardService).recordWin(user, "game-2", BoardSize.MEDIUM);
    }

    @Test
    void givenNullPrincipalButCurrentUserResolved_whenRecordWin_thenStillUseFallbackContext() {
        // Given
        User user = new User();
        UserService.UserContext context = new UserService.UserContext("fallback@example.com");
        when(userService.resolveUser((Principal) null)).thenReturn(Optional.empty());
        when(userService.resolveOAuthUser()).thenReturn("Neprihlásený používateľ");
        when(userService.currentUser()).thenReturn(context);
        when(userService.resolveUser(context)).thenReturn(Optional.of(user));

        // When
        leaderboardController.recordWin(null, 18, "game-3");

        // Then
        verify(leaderboardService).recordWin(user, "game-3", BoardSize.LARGE);
    }

    @Test
    void givenUnresolvedUsers_whenRecordWin_thenThrowUnauthorized() {
        // Given
        UserService.UserContext context = new UserService.UserContext("none@example.com");
        when(userService.resolveUser(principal)).thenReturn(Optional.empty());
        when(userService.resolveOAuthUser()).thenReturn("Neprihlásený používateľ");
        when(userService.currentUser()).thenReturn(context);
        when(userService.resolveUser(context)).thenReturn(Optional.empty());

        // When / Then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> leaderboardController.recordWin(principal, 18, "game-4")
        );
        assertThat(ex.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void givenOAuthUserMaterializedDuringCall_whenRecordWin_thenResolveAndSaveWin() {
        // Given
        User user = new User();
        when(userService.resolveOAuthUser()).thenReturn("Prihlásený používateľ: OAuth");
        when(userService.resolveUser(principal))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(user));

        // When
        leaderboardController.recordWin(principal, 12, "game-5");

        // Then
        verify(userService).resolveOAuthUser();
        verify(userService, times(2)).resolveUser(principal);
        verify(leaderboardService).recordWin(user, "game-5", BoardSize.SMALL);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11, 13, 14, 16, 17, 19, 100})
    void givenInvalidBoardSize_whenRecordWin_thenThrowIllegalArgument(int invalidSize) {
        // Given / When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> leaderboardController.recordWin(principal, invalidSize, "game-invalid")
        );

        assertThat(ex.getMessage()).contains("Invalid board size");
        verifyNoInteractions(leaderboardService);
    }

    @Test
    void givenPagination_whenLeaderboard_thenReturnServiceResponse() {
        // Given
        List<LeaderboardService.Response> expected = List.of(new LeaderboardService.Response("A", 1, 2, 3, 14));
        when(leaderboardService.leaderboard(2, 25)).thenReturn(expected);

        // When
        List<LeaderboardService.Response> actual = leaderboardController.leaderboard(2, 25);

        // Then
        assertThat(actual).isEqualTo(expected);
    }
}
