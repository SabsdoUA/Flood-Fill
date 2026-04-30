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
public class StartGameUseCase {

    private final Ports.GameRepository repo;
    private final Ports.BoardGenerator generator;
    private final Ports.MoveLimitEstimator moveLimitEstimator;
    private final GameStateMapper mapper;

    public GameResponse execute(GameCommands.StartGame cmd, String ownerIdentity) {
        var board = generator.generate(cmd.size());
        int limit = moveLimitEstimator.estimateMoveLimit(board);
        var state = new GameState.Active(cmd.gameId(), ownerIdentity, board, 0, limit);
        repo.save(state);
        return mapper.toResponse(state);
    }
}
