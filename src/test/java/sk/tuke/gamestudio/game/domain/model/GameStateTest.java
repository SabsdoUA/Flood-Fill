package sk.tuke.gamestudio.game.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateTest {

    private static Board board(Color color) {
        return new Board(new Color[][]{{color}}, 1);
    }

    @ParameterizedTest
    @CsvSource({
            "true,3,10,Won",
            "false,9,10,Lost",
            "false,3,10,Active"
    })
    void givenActiveState_whenTransition_thenReturnExpectedStateType(boolean won, int movesTaken, int limit, String expectedType) {
        GameState.Active active = new GameState.Active("g-1", "qa@example.com", board(Color.RED), movesTaken, limit);

        GameState next = active.transition(board(Color.BLUE), won);

        assertThat(next.movesTaken()).isEqualTo(movesTaken + 1);
        assertThat(next.moveLimit()).isEqualTo(limit);
        assertThat(next.gameId()).isEqualTo("g-1");
        assertThat(next.ownerIdentity()).isEqualTo("qa@example.com");
        assertThat(next.board().at(0, 0)).isEqualTo(Color.BLUE);
        assertThat(next.getClass().getSimpleName()).isEqualTo(expectedType);
    }
}
