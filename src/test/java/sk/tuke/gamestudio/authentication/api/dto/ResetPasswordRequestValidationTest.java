package sk.tuke.gamestudio.authentication.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ResetPasswordRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @CsvSource({
            "'',StrongPass1",
            "token,''"
    })
    void givenBlankTokenOrPassword_whenValidate_thenViolationExists(String token, String password) {
        ResetPasswordRequest request = validRequest();
        request.setToken(token);
        request.setNewPassword(password);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "short7",
            "OnlyLetters",
            "12345678"
    })
    void givenInvalidPasswordFormat_whenValidate_thenPasswordViolationExists(String password) {
        ResetPasswordRequest request = validRequest();
        request.setNewPassword(password);

        assertThat(validator.validate(request))
                .anyMatch(v -> "newPassword".equals(v.getPropertyPath().toString()));
    }

    private static ResetPasswordRequest validRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token");
        request.setNewPassword("StrongPass1");
        return request;
    }
}
