package sk.tuke.gamestudio.authentication.api.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    @Test
    void givenValues_whenSettersCalled_thenGettersReturnSameValues() {
        // Given
        RegisterRequest request = new RegisterRequest();

        // When
        request.setEmail("qa@gmail.com");
        request.setNickname("qa_user");
        request.setPassword("StrongPass1");

        // Then
        assertThat(request.getEmail()).isEqualTo("qa@gmail.com");
        assertThat(request.getNickname()).isEqualTo("qa_user");
        assertThat(request.getPassword()).isEqualTo("StrongPass1");
    }
}
