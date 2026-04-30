package sk.tuke.gamestudio.authentication.core.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthProviderTest {

    @Test
    void givenEnum_whenInspectValues_thenContainsExpectedProviders() {
        assertThat(AuthProvider.values()).containsExactly(AuthProvider.LOCAL, AuthProvider.GOOGLE);
        assertThat(AuthProvider.valueOf("GOOGLE")).isEqualTo(AuthProvider.GOOGLE);
    }
}
