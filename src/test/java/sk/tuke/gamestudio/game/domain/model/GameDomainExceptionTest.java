package sk.tuke.gamestudio.game.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameDomainExceptionTest {

    @Test
    void givenSpecificExceptionTypes_whenCreated_thenMessagesAreCorrect() {
        assertThat(new GameDomainException.NotFound("abc")).hasMessage("Game not found: abc");
        assertThat(new GameDomainException.Forbidden("abc")).hasMessage("Game access denied: abc");
        assertThat(new GameDomainException.AlreadyWon()).hasMessage("Game already won");
        assertThat(new GameDomainException.MoveLimitReached()).hasMessage("Move limit reached");
        assertThat(new GameDomainException.InvalidColor("pink")).hasMessage("Invalid color: pink");
        assertThat(new GameDomainException.InvalidSize(0)).hasMessage("Invalid board size: 0");
    }
}
