package sk.tuke.gamestudio.game.application;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class GameRecordsAndCommandsTest {

    @Test
    void givenCommandRecords_whenCreated_thenExposeValues() {
        var start = new GameCommands.StartGame("g", 12);
        var resume = new GameCommands.ResumeGame("g", 15);
        var move = new GameCommands.MakeMove("g", "RED");

        assertThat(start.gameId()).isEqualTo("g");
        assertThat(start.size()).isEqualTo(12);
        assertThat(resume.size()).isEqualTo(15);
        assertThat(move.color()).isEqualTo("RED");
    }

    @Test
    void givenResponseRecord_whenCreated_thenExposeValues() {
        var response = new GameResponse("g", new String[][]{{"RED"}}, 1, 3, "ACTIVE", false, null);

        assertThat(response.gameId()).isEqualTo("g");
        assertThat(response.movesTaken()).isEqualTo(1);
        assertThat(response.moveLimit()).isEqualTo(3);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void givenUtilityClass_whenReflectivelyInstantiate_thenConstructorIsAccessibleForCoverage() throws Exception {
        Constructor<GameCommands> ctor = GameCommands.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        Object instance = ctor.newInstance();

        assertThat(instance).isInstanceOf(GameCommands.class);
    }
}
