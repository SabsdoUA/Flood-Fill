package sk.tuke.gamestudio.game.domain.service;

import org.junit.jupiter.api.Test;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;

import static org.assertj.core.api.Assertions.assertThat;

class WinCheckerTest {

    @Test
    void givenUniformBoard_whenCheckWin_thenTrue() {
        Board board = new Board(new Color[][]{{Color.RED, Color.RED}, {Color.RED, Color.RED}}, 2);

        assertThat(WinChecker.isWon(board)).isTrue();
    }

    @Test
    void givenMixedBoard_whenCheckWin_thenFalse() {
        Board board = new Board(new Color[][]{{Color.RED, Color.BLUE}, {Color.RED, Color.RED}}, 2);

        assertThat(WinChecker.isWon(board)).isFalse();
    }
}
