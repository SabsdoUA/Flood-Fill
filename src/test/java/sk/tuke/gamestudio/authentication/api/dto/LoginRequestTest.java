package sk.tuke.gamestudio.authentication.api.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    @Test
    void givenValues_whenSettersCalled_thenGettersReturnSameValues() {
        // Given
        LoginRequest request = new LoginRequest();

        // When
        request.setEmail("qa@gmail.com");
        request.setPassword("StrongPass1");

        // Then
        assertThat(request.getEmail()).isEqualTo("qa@gmail.com");
        assertThat(request.getPassword()).isEqualTo("StrongPass1");
    }
}
