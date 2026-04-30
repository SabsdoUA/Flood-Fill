package sk.tuke.gamestudio.authentication.core.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @ParameterizedTest
    @CsvSource(value = {
            "nick|name|mail@gmail.com|nick",
            " |John|mail@gmail.com|John",
            " | |mail@gmail.com|mail@gmail.com",
            "__NULL__|  Jane  |mail@gmail.com|Jane",
            "__NULL__|__NULL__|mail@gmail.com|mail@gmail.com"
    }, delimiter = '|')
    void givenDifferentIdentityFields_whenDisplayName_thenPickFirstNonBlank(String nickname, String name, String email, String expected) {
        User user = new User();
        user.setNickname("__NULL__".equals(nickname) ? null : nickname);
        user.setName("__NULL__".equals(name) ? null : name);
        user.setEmail(email);

        assertThat(user.displayName()).isEqualTo(expected);
    }
}
