package sk.tuke.gamestudio.leaderboard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.authentication.core.service.UserService;
import sk.tuke.gamestudio.leaderboard.model.UserStats.BoardSize;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final UserService userService;

    // ── Zápis výhry ───────────────────────────────────────────────────────────
    @PostMapping("/win")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordWin(
            Principal principal,
            @RequestParam @Min(1) int size,
            @RequestParam @NotBlank String gameId
    ) {
        var boardSize = BoardSize.fromValue(size)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid board size: %d. Allowed: %s"
                                .formatted(size, Arrays.toString(BoardSize.allowedValues()))
                ));

        var user = userService.resolveUser(principal)
                .or(() -> {
                    // Ensure OAuth users are materialized in DB even if this is their first API call
                    // after provider redirect.
                    userService.resolveOAuthUser();
                    return userService.resolveUser(principal);
                })
                .or(() -> userService.resolveUser(userService.currentUser()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Neprihlásený používateľ"
                ));

        leaderboardService.recordWin(user, gameId, boardSize);
    }

    // ── Čítanie rebríčka ──────────────────────────────────────────────────────
    @GetMapping
    public List<LeaderboardService.Response> leaderboard(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page index must be >= 0")
            int page,
            @RequestParam(defaultValue = "50")
            @Min(value = 1, message = "Page size must be >= 1")
            @Max(value = 100, message = "Page size must be <= 100")
            int size
    ) {
        return leaderboardService.leaderboard(page, size);
    }
}
