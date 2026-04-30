package sk.tuke.gamestudio.authentication.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@gmail.com", "user.name123@gmail.com"})
    void givenValidEmails_whenValidate_thenNoEmailViolation(String email) {
        RegisterRequest req = validRequest();
        req.setEmail(email);

        var violations = validator.validate(req);

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "user@yahoo.com", "badmail", "user@gmail.co"})
    void givenInvalidEmailBoundaryValues_whenValidate_thenEmailViolationExists(String email) {
        RegisterRequest req = validRequest();
        req.setEmail(email);

        var violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "thisnicknameiswaytoolongfortherule123", "bad nick"})
    void givenInvalidNickname_whenValidate_thenNicknameViolationExists(String nickname) {
        RegisterRequest req = validRequest();
        req.setNickname(nickname);

        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("nickname"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1A", "nocapital123", "NOLOWER123"})
    void givenInvalidPasswordBoundaryValues_whenValidate_thenPasswordViolationExists(String password) {
        RegisterRequest req = validRequest();
        req.setPassword(password);

        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    private static RegisterRequest validRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("valid@gmail.com");
        req.setNickname("valid_user");
        req.setPassword("StrongPass1");
        return req;
    }
}
