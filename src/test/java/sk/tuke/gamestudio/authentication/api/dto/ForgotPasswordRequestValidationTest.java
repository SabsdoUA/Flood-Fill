package sk.tuke.gamestudio.authentication.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ForgotPasswordRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plain", "bad@", "user @gmail.com"})
    void givenInvalidEmail_whenValidate_thenViolationExists(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);

        assertThat(validator.validate(request))
                .anyMatch(v -> "email".equals(v.getPropertyPath().toString()));
    }
}
