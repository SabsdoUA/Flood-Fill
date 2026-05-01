package sk.tuke.gamestudio.game.application;

import org.springframework.stereotype.Component;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameState;

@Component
public class GameStateMapper {

    public GameResponse toResponse(GameState state) {
        return new GameResponse(
                state.gameId(),
                toGrid(state.board().grid()),
                state.movesTaken(),
                state.moveLimit(),
                toStatus(state),
                state instanceof GameState.Won,
                null
        );
    }

    private static String toStatus(GameState s) {
        return switch (s) {
            case GameState.Active e -> "ACTIVE";
            case GameState.Won    e -> "WON";
            case GameState.Lost   e -> "LOST";
        };
    }

    private static String[][] toGrid(Color[][] grid) {
        int n = grid.length;
        var out = new String[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                out[i][j] = grid[i][j].name();
        return out;
    }
}
