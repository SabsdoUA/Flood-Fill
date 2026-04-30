package sk.tuke.gamestudio.leaderboard.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sk.tuke.gamestudio.leaderboard.model.UserStats.BoardSize;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserStatsTest {

    @ParameterizedTest
    @CsvSource({
            "12,SMALL",
            "15,MEDIUM",
            "18,LARGE"
    })
    void givenAllowedBoardSizeValue_whenFromValue_thenResolveEnum(int value, BoardSize expected) {
        // When
        Optional<BoardSize> actual = BoardSize.fromValue(value);

        // Then
        assertThat(actual).contains(expected);
    }

    @ParameterizedTest
    @CsvSource({"0", "11", "13", "14", "16", "17", "19"})
    void givenUnsupportedBoardSizeValue_whenFromValue_thenReturnEmpty(int value) {
        // When
        Optional<BoardSize> actual = BoardSize.fromValue(value);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    void givenBoardSizeEnum_whenAllowedValues_thenReturnExpectedOrder() {
        // When
        int[] values = BoardSize.allowedValues();

        // Then
        assertThat(values).containsExactly(12, 15, 18);
    }

    @Test
    void givenUserConstructor_whenCreateUserStats_thenSetIdAndDefaultIsNew() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        UserStats stats = new UserStats(id);

        // Then
        assertThat(stats.getId()).isEqualTo(id);
        assertThat(stats.getVersion()).isZero();
        assertThat(stats.isNew()).isTrue();
    }

    @Test
    void givenPersistedEntity_whenMarkNotNew_thenIsNewFalse() {
        // Given
        UserStats stats = new UserStats();
        assertThat(stats.isNew()).isTrue();

        // When
        stats.markNotNew();

        // Then
        assertThat(stats.isNew()).isFalse();
    }

    @Test
    void givenWins_whenTotalPoints_thenCalculateWeightedSum() {
        // Given
        UserStats stats = new UserStats();
        stats.setSmallWins(2);
        stats.setMediumWins(3);
        stats.setLargeWins(4);

        // When
        int points = stats.totalPoints();

        // Then
        assertThat(points).isEqualTo(2 + 2 * 3 + 3 * 4);
    }
}
