package sk.tuke.gamestudio.game.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sk.tuke.gamestudio.authentication.core.service.UserService;
import sk.tuke.gamestudio.game.application.GameCommands;
import sk.tuke.gamestudio.game.application.GameResponse;
import sk.tuke.gamestudio.game.application.usecase.MakeMoveUseCase;
import sk.tuke.gamestudio.game.application.usecase.ResumeGameUseCase;
import sk.tuke.gamestudio.game.application.usecase.StartGameUseCase;
import sk.tuke.gamestudio.leaderboard.LeaderboardService;
import sk.tuke.gamestudio.leaderboard.model.UserStats.BoardSize;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameController {

    private final StartGameUseCase startGame;
    private final ResumeGameUseCase resumeGame;
    private final MakeMoveUseCase makeMove;
    private final UserService userService;
    private final LeaderboardService leaderboardService;

    @PostMapping("/{gameId}/start")
    public ResponseEntity<GameResponse> start(
            @PathVariable String gameId,
            Principal principal,
            @Valid @RequestBody StartBody body) {
        return ResponseEntity.ok(
                startGame.execute(new GameCommands.StartGame(gameId, body.size()), resolveOwnerIdentity(principal)));
    }

    @PostMapping("/{gameId}/resume")
    public ResponseEntity<GameResponse> resume(
            @PathVariable String gameId,
            Principal principal,
            @Valid @RequestBody StartBody body) {
        var response = resumeGame.execute(new GameCommands.ResumeGame(gameId, body.size()), resolveOwnerIdentity(principal));
        recordWinIfNeeded(principal, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{gameId}/move")
    public ResponseEntity<GameResponse> move(
            @PathVariable String gameId,
            Principal principal,
            @Valid @RequestBody MoveBody body) {
        var response = makeMove.execute(new GameCommands.MakeMove(gameId, body.color()), resolveOwnerIdentity(principal));
        recordWinIfNeeded(principal, response);
        return ResponseEntity.ok(response);
    }

    private String resolveOwnerIdentity(Principal principal) {
        return userService.resolveIdentity(principal)
                .or(() -> java.util.Optional.ofNullable(userService.currentUser()).map(UserService.UserContext::email))
                .orElse(null);
    }

    private void recordWinIfNeeded(Principal principal, GameResponse response) {
        if (!response.won() || response.grid() == null) {
            return;
        }

        var boardSize = BoardSize.fromValue(response.grid().length);
        if (boardSize.isEmpty()) {
            return;
        }

        resolveCurrentUser(principal)
                .ifPresent(user -> leaderboardService.recordWin(user, response.gameId(), boardSize.get()));
    }

    private Optional<sk.tuke.gamestudio.authentication.core.model.User> resolveCurrentUser(Principal principal) {
        return userService.resolveUser(principal)
                .or(() -> {
                    userService.resolveOAuthUser();
                    return userService.resolveUser(principal);
                })
                .or(() -> userService.resolveUser(userService.currentUser()));
    }

    private record StartBody(@Min(1) int size) {
    }

    private record MoveBody(@NotBlank String color) {
    }
}
