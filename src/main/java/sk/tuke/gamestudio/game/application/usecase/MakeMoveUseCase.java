package sk.tuke.gamestudio.game.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sk.tuke.gamestudio.game.application.GameCommands;
import sk.tuke.gamestudio.game.application.GameResponse;
import sk.tuke.gamestudio.game.application.GameStateMapper;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameDomainException;
import sk.tuke.gamestudio.game.domain.model.GameState;
import sk.tuke.gamestudio.game.domain.port.Ports;
import sk.tuke.gamestudio.game.domain.service.FloodFill;
import sk.tuke.gamestudio.game.domain.service.WinChecker;

@Service
@RequiredArgsConstructor
public class MakeMoveUseCase {

    private final Ports.GameRepository repo;
    private final GameStateMapper mapper;

    public GameResponse execute(GameCommands.MakeMove cmd, String ownerIdentity) {
        var state = repo.findById(cmd.gameId())
                .orElseThrow(() -> new GameDomainException.NotFound(cmd.gameId()));
        requireOwner(state, ownerIdentity);
        var active = requireActive(state);
        var color = Color.fromString(cmd.color())
                .orElseThrow(() -> new GameDomainException.InvalidColor(cmd.color()));
        if (active.board().at(0, 0) == color) return mapper.toResponse(active);
        var newGrid = FloodFill.apply(active.board().grid(), 0, 0, color);
        var newBoard = new Board(newGrid, newGrid.length);
        var next = active.transition(newBoard, WinChecker.isWon(newBoard));

        repo.save(next);
        return mapper.toResponse(next);
    }

    private static void requireOwner(GameState state, String ownerIdentity) {
        if (state.ownerIdentity() != null && !java.util.Objects.equals(state.ownerIdentity(), ownerIdentity)) {
            throw new GameDomainException.Forbidden(state.gameId());
        }
    }

    private static GameState.Active requireActive(GameState s) {
        return switch (s) {
            case GameState.Active a -> a;
            case GameState.Won __ -> throw new GameDomainException.AlreadyWon();
            case GameState.Lost __ -> throw new GameDomainException.MoveLimitReached();
        };
    }
}
