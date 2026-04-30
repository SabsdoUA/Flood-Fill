package sk.tuke.gamestudio.leaderboard.model;

import org.junit.jupiter.api.Test;
import sk.tuke.gamestudio.leaderboard.model.UserStats.BoardSize;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecordedWinTest {

    @Test
    void givenConstructorArguments_whenCreateRecordedWin_thenExposeValues() {
        UUID userId = UUID.randomUUID();

        RecordedWin recordedWin = new RecordedWin(userId, "game-1", BoardSize.MEDIUM);

        assertThat(recordedWin.getUserId()).isEqualTo(userId);
        assertThat(recordedWin.getGameId()).isEqualTo("game-1");
        assertThat(recordedWin.getBoardSize()).isEqualTo(BoardSize.MEDIUM);
        assertThat(recordedWin.getRecordedAt()).isNotNull();
    }
}
