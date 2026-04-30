package sk.tuke.gamestudio.feedback.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreateFeedbackRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @CsvSource({
            "1,true",
            "5,true",
            "0,false",
            "6,false"
    })
    void givenBoundaryRatingValues_whenValidate_thenMatchExpectedResult(Integer rating, boolean valid) {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest(rating, "comment");

        // When
        Set<ConstraintViolation<CreateFeedbackRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations.isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({
            "149,true",
            "150,true",
            "151,false"
    })
    void givenBoundaryCommentLengths_whenValidate_thenMatchExpectedResult(int commentLength, boolean valid) {
        // Given
        String comment = "x".repeat(commentLength);
        CreateFeedbackRequest request = new CreateFeedbackRequest(5, comment);

        // When
        Set<ConstraintViolation<CreateFeedbackRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations.isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({
            "true",
            "false"
    })
    void givenNullableComment_whenValidate_thenAlwaysValid(boolean commentPresent) {
        // Given
        String comment = commentPresent ? "ok" : null;
        CreateFeedbackRequest request = new CreateFeedbackRequest(5, comment);

        // When
        Set<ConstraintViolation<CreateFeedbackRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "true",
            "false"
    })
    void givenNullRating_whenValidate_thenAlwaysInvalid(boolean withComment) {
        // Given
        String comment = withComment ? "ok" : null;
        CreateFeedbackRequest request = new CreateFeedbackRequest(null, comment);

        // When
        Set<ConstraintViolation<CreateFeedbackRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> "rating".equals(v.getPropertyPath().toString()));
    }
}
