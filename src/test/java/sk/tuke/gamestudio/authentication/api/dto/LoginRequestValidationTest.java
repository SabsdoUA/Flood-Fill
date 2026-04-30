package sk.tuke.gamestudio.authentication.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "user@yahoo.com", "plain"})
    void givenInvalidEmail_whenValidate_thenEmailViolationExists(String email) {
        LoginRequest req = validRequest();
        req.setEmail(email);

        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", ""})
    void givenInvalidPassword_whenValidate_thenPasswordViolationExists(String password) {
        LoginRequest req = validRequest();
        req.setPassword(password);

        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    private static LoginRequest validRequest() {
        LoginRequest req = new LoginRequest();
        req.setEmail("valid@gmail.com");
        req.setPassword("StrongPass1");
        return req;
    }
}
