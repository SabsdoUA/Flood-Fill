package sk.tuke.gamestudio.game.infrastructure.engine;

import org.junit.jupiter.api.Test;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;

import static org.assertj.core.api.Assertions.assertThat;

class DsuGreedyMoveLimitEstimatorTest {

    private final DsuGreedyMoveLimitEstimator estimator = new DsuGreedyMoveLimitEstimator();

    @Test
    void givenSingleColorBoard_whenEstimateGreedyMoves_thenZero() {
        Board board = new Board(new Color[][]{{Color.RED, Color.RED}, {Color.RED, Color.RED}}, 2);

        assertThat(estimator.estimateGreedyMoves(board)).isZero();
    }

    @Test
    void givenTwoRegionsBoard_whenEstimateGreedyMoves_thenOne() {
        Board board = new Board(new Color[][]{{Color.RED, Color.BLUE}, {Color.RED, Color.BLUE}}, 2);

        assertThat(estimator.estimateGreedyMoves(board)).isEqualTo(1);
    }

    @Test
    void givenCheckerboard_whenEstimateGreedyMoves_thenNeedsAtLeastTwoMoves() {
        Board board = new Board(new Color[][]{
                {Color.RED, Color.BLUE, Color.RED},
                {Color.BLUE, Color.RED, Color.BLUE},
                {Color.RED, Color.BLUE, Color.RED}
        }, 3);

        assertThat(estimator.estimateGreedyMoves(board)).isGreaterThanOrEqualTo(2);
    }

    @Test
    void givenSingleColorBoard_whenEstimateMoveLimit_thenKeepAtLeastOneMove() {
        Board board = new Board(new Color[][]{{Color.RED, Color.RED}, {Color.RED, Color.RED}}, 2);

        assertThat(estimator.estimateMoveLimit(board)).isEqualTo(1);
    }

    @Test
    void givenGreedyEstimate_whenEstimateMoveLimit_thenAddSafetyMargin() {
        Board board = new Board(new Color[][]{{Color.RED, Color.BLUE}, {Color.RED, Color.BLUE}}, 2);

        assertThat(estimator.estimateMoveLimit(board)).isEqualTo(2);
    }
}
