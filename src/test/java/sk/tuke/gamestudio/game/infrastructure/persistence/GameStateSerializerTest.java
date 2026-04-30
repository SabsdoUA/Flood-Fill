package sk.tuke.gamestudio.game.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameState;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class GameStateSerializerTest {

    static Stream<GameState> states() {
        Board board = new Board(new Color[][]{{Color.RED, Color.BLUE}, {Color.GREEN, Color.YELLOW}}, 2);
        return Stream.of(
                new GameState.Active("id", "qa@example.com", board, 1, 10),
                new GameState.Won("id", "qa@example.com", board, 2, 10),
                new GameState.Lost("id", "qa@example.com", board, 10, 10)
        );
    }

    @ParameterizedTest
    @MethodSource("states")
    void givenState_whenSerializeDeserialize_thenRoundTripPreservesData(GameState state) {
        byte[] data = GameStateSerializer.serialize(state);

        GameState restored = GameStateSerializer.deserialize("id", data);

        assertThat(restored).isEqualTo(state);
    }

    @Test
    void givenUnknownFormatVersion_whenDeserialize_thenThrow() {
        byte[] data = GameStateSerializer.serialize(new GameState.Active(
                "id", "qa@example.com", new Board(new Color[][]{{Color.RED}}, 1), 0, 1));
        data[0] = 99;

        assertThatThrownBy(() -> GameStateSerializer.deserialize("id", data))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown board format version");
    }

    @Test
    void givenUnknownStatusByte_whenDeserialize_thenThrow() {
        byte[] data = GameStateSerializer.serialize(new GameState.Active(
                "id", "qa@example.com", new Board(new Color[][]{{Color.RED}}, 1), 0, 1));
        data[1] = 99;

        assertThatThrownBy(() -> GameStateSerializer.deserialize("id", data))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown status byte");
    }

    @Test
    void givenLegacyVersionOnePayload_whenDeserialize_thenRestoreStateWithoutOwner() {
        byte[] data = {
                1, 0, 1,
                0, 0, 0, 2,
                0, 0, 0, 5,
                0
        };

        GameState restored = GameStateSerializer.deserialize("id", data);

        assertThat(restored).isEqualTo(new GameState.Active("id", null, new Board(new Color[][]{{Color.RED}}, 1), 2, 5));
    }

    @Test
    void givenSerializerUtilityClass_whenReflectivelyInstantiate_thenConstructorCovered() throws Exception {
        var ctor = GameStateSerializer.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isInstanceOf(GameStateSerializer.class);
    }
}
