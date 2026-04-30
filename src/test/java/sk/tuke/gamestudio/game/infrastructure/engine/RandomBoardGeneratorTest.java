package sk.tuke.gamestudio.game.infrastructure.engine;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameDomainException;

import static org.assertj.core.api.Assertions.*;

class RandomBoardGeneratorTest {

    private final RandomBoardGenerator generator = new RandomBoardGenerator();

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 1, 11, 13, 14, 16, 17, 19, Integer.MAX_VALUE})
    void givenInvalidSize_whenGenerate_thenThrowInvalidSize(int size) {
        assertThatThrownBy(() -> generator.generate(size))
                .isInstanceOf(GameDomainException.InvalidSize.class)
                .hasMessage("Invalid board size: " + size);
    }

    @ParameterizedTest
    @ValueSource(ints = {12, 15, 18})
    void givenValidSize_whenGenerate_thenBoardIsSquareAndFullyColored(int size) {
        Board board = generator.generate(size);

        assertThat(board.size()).isEqualTo(size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                assertThat(board.at(r, c)).isIn(Color.values());
            }
        }
    }
}
