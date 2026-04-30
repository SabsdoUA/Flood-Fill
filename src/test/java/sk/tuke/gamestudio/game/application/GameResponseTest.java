package sk.tuke.gamestudio.game.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameResponseTest {

    @Test
    void givenSameValues_whenCompareRecords_thenUseValueSemantics() {
        GameResponse left = new GameResponse("g1", new String[][]{{"RED"}}, 1, 5, "ACTIVE", false, null);
        GameResponse right = new GameResponse("g1", new String[][]{{"RED"}}, 1, 5, "ACTIVE", false, null);

        assertThat(left.gameId()).isEqualTo("g1");
        assertThat(left.movesTaken()).isEqualTo(1);
        assertThat(left.moveLimit()).isEqualTo(5);
        assertThat(left.status()).isEqualTo("ACTIVE");
        assertThat(left.won()).isFalse();
        assertThat(left.error()).isNull();
        assertThat(left).usingRecursiveComparison().isEqualTo(right);
    }
}
