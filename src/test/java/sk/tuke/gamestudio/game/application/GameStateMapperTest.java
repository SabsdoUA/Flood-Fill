package sk.tuke.gamestudio.game.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameState;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateMapperTest {

    private final GameStateMapper mapper = new GameStateMapper() {};

    static Stream<GameState> states() {
        Board board = new Board(new Color[][]{{Color.RED, Color.BLUE}, {Color.GREEN, Color.YELLOW}}, 2);
        return Stream.of(
                new GameState.Active("a", "qa@example.com", board, 1, 5),
                new GameState.Won("w", "qa@example.com", board, 2, 5),
                new GameState.Lost("l", "qa@example.com", board, 5, 5)
        );
    }

    @ParameterizedTest
    @MethodSource("states")
    void givenAnyState_whenMapToResponse_thenMapGridAndStatus(GameState state) {
        GameResponse response = mapper.toResponse(state);

        assertThat(response.gameId()).isEqualTo(state.gameId());
        assertThat(response.movesTaken()).isEqualTo(state.movesTaken());
        assertThat(response.moveLimit()).isEqualTo(state.moveLimit());
        assertThat(response.error()).isNull();
        assertThat(Arrays.deepEquals(response.grid(), new String[][]{
                {"RED", "BLUE"},
                {"GREEN", "YELLOW"}
        })).isTrue();
        assertThat(response.status()).isIn("ACTIVE", "WON", "LOST");
        assertThat(response.won()).isEqualTo(state instanceof GameState.Won);
    }
}
