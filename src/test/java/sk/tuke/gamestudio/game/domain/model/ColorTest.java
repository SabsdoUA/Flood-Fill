package sk.tuke.gamestudio.game.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ColorTest {

    @ParameterizedTest
    @CsvSource({
            "RED,RED",
            "red,RED",
            "ReD,RED",
            "blue,BLUE",
            "YELLOW,YELLOW"
    })
    void givenDifferentCaseNames_whenFromString_thenReturnColor(String input, Color expected) {
        assertThat(Color.fromString(input)).contains(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "unknown", "pink"})
    void givenInvalidName_whenFromString_thenReturnEmpty(String input) {
        assertThat(Color.fromString(input)).isEmpty();
    }
}
