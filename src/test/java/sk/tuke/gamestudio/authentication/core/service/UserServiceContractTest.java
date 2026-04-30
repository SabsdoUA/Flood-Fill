package sk.tuke.gamestudio.authentication.core.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceContractTest {

    @Test
    void givenUserContextRecord_whenCreated_thenExposeEmail() {
        UserService.UserContext context = new UserService.UserContext("user@gmail.com");

        assertThat(context.email()).isEqualTo("user@gmail.com");
    }
}
