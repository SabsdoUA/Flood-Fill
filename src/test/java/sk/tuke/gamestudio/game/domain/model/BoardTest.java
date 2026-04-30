package sk.tuke.gamestudio.game.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BoardTest {

    @Test
    void givenNullGrid_whenCreateBoard_thenThrowNpe() {
        assertThatThrownBy(() -> new Board(null, 2))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("grid must not be null");
    }

    @Test
    void givenInvalidSize_whenCreateBoard_thenThrowIae() {
        Color[][] grid = {{Color.RED}};

        assertThatThrownBy(() -> new Board(grid, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid board size");

        assertThatThrownBy(() -> new Board(grid, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid board size");
    }

    @Test
    void givenInvalidRow_whenCreateBoard_thenThrowIae() {
        Color[][] gridWithNullRow = {null, {Color.RED, Color.BLUE}};
        Color[][] gridWithBadRowLength = {{Color.RED}, {Color.BLUE, Color.GREEN}};

        assertThatThrownBy(() -> new Board(gridWithNullRow, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid row at index: 0");

        assertThatThrownBy(() -> new Board(gridWithBadRowLength, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid row at index: 0");
    }

    @Test
    void givenValidGrid_whenCreateBoard_thenDefensivelyCopyAndExposeReadOnlyAccess() {
        Color[][] source = {
                {Color.RED, Color.BLUE},
                {Color.GREEN, Color.YELLOW}
        };

        Board board = new Board(source, 2);
        source[0][0] = Color.PURPLE;

        assertThat(board.at(0, 0)).isEqualTo(Color.RED);

        Color[][] firstGrid = board.grid();
        firstGrid[0][1] = Color.ORANGE;

        Color[][] secondGrid = board.grid();
        assertThat(secondGrid[0][1]).isEqualTo(Color.BLUE);
    }

    @Test
    void givenEqualBoards_whenCompare_thenEqualsHashCodeAndToStringAreStable() {
        Color[][] gridA = {
                {Color.RED, Color.BLUE},
                {Color.GREEN, Color.YELLOW}
        };
        Color[][] gridB = {
                {Color.RED, Color.BLUE},
                {Color.GREEN, Color.YELLOW}
        };

        Board a = new Board(gridA, 2);
        Board b = new Board(gridB, 2);

        assertThat(a)
                .isEqualTo(a)
                .isEqualTo(b)
                .isNotEqualTo(null)
                .isNotEqualTo("not-board");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("Board[size=2", "RED", "YELLOW");
    }
}
