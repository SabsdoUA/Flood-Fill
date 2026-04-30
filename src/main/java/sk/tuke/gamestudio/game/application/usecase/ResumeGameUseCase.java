package sk.tuke.gamestudio.game.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sk.tuke.gamestudio.game.application.GameCommands;
import sk.tuke.gamestudio.game.application.GameResponse;
import sk.tuke.gamestudio.game.application.GameStateMapper;
import sk.tuke.gamestudio.game.domain.model.GameState;
import sk.tuke.gamestudio.game.domain.port.Ports;

@Service
@RequiredArgsConstructor
public class ResumeGameUseCase {

    private final Ports.GameRepository repo;
    private final Ports.BoardGenerator generator;
    private final Ports.MoveLimitEstimator moveLimitEstimator;
    private final GameStateMapper mapper;

    public GameResponse execute(GameCommands.ResumeGame cmd, String ownerIdentity) {
        return repo.findById(cmd.gameId())
                .map(state -> requireOwner(state, ownerIdentity))
                .map(mapper::toResponse)
                .orElseGet(() -> startNew(cmd, ownerIdentity));
    }

    private GameResponse startNew(GameCommands.ResumeGame cmd, String ownerIdentity) {
        var board = generator.generate(cmd.size());
        int limit = moveLimitEstimator.estimateMoveLimit(board);
        var state = new GameState.Active(cmd.gameId(), ownerIdentity, board, 0, limit);
        repo.save(state);
        return mapper.toResponse(state);
    }

    private static GameState requireOwner(GameState state, String ownerIdentity) {
        if (state.ownerIdentity() == null || java.util.Objects.equals(state.ownerIdentity(), ownerIdentity)) {
            return state;
        }
        throw new sk.tuke.gamestudio.game.domain.model.GameDomainException.Forbidden(state.gameId());
    }
}
