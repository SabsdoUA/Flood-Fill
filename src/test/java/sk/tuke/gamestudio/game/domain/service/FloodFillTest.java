package sk.tuke.gamestudio.game.domain.service;

import org.junit.jupiter.api.Test;
import sk.tuke.gamestudio.game.domain.model.Color;

import static org.assertj.core.api.Assertions.assertThat;

class FloodFillTest {

    @Test
    void givenSameColor_whenApply_thenReturnSameReferenceAndNoMutation() {
        Color[][] grid = {
                {Color.RED, Color.BLUE},
                {Color.BLUE, Color.RED}
        };

        Color[][] result = FloodFill.apply(grid, 0, 0, Color.RED);

        assertThat(result).isSameAs(grid);
        assertThat(grid[0][0]).isEqualTo(Color.RED);
    }

    @Test
    void givenConnectedRegion_whenApply_thenFillOnlyConnectedCells() {
        Color[][] grid = {
                {Color.RED, Color.RED, Color.BLUE},
                {Color.RED, Color.BLUE, Color.BLUE},
                {Color.GREEN, Color.GREEN, Color.BLUE}
        };

        Color[][] result = FloodFill.apply(grid, 0, 0, Color.YELLOW);

        assertThat(result[0][0]).isEqualTo(Color.YELLOW);
        assertThat(result[0][1]).isEqualTo(Color.YELLOW);
        assertThat(result[1][0]).isEqualTo(Color.YELLOW);
        assertThat(result[1][1]).isEqualTo(Color.BLUE);
        assertThat(result[2][0]).isEqualTo(Color.GREEN);

        assertThat(grid[0][0]).isEqualTo(Color.RED);
    }
}
