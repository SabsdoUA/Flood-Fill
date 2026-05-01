package sk.tuke.gamestudio.game.domain.port;

import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.GameState;

import java.util.Optional;
import java.util.function.Supplier;

public final class Ports {

    private Ports() {}

    public interface GameRepository {
        void save(GameState state);
        Optional<GameState> findById(String gameId);
        void delete(String gameId);
    }

    public interface GameLock {
        <T> T withLock(String gameId, Supplier<T> action);
    }

    public interface BoardGenerator {
        Board generate(int size);
    }

    public interface MoveLimitEstimator {
        int estimateMoveLimit(Board board);
    }
}
