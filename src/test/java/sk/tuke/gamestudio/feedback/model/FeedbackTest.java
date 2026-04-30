package sk.tuke.gamestudio.feedback.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackTest {

    @Test
    void givenNullDates_whenPrePersist_thenInitializeCreatedFields() {
        // Given
        Feedback feedback = new Feedback();

        // When
        feedback.prePersist();

        // Then
        assertThat(feedback.getCreatedAt()).isNotNull();
        assertThat(feedback.getCreatedDate()).isNotNull();
    }

    @Test
    void givenExistingDates_whenPrePersist_thenKeepOriginalValues() {
        // Given
        Feedback feedback = new Feedback();
        Instant createdAt = Instant.parse("2026-03-20T10:00:00Z");
        LocalDate createdDate = LocalDate.parse("2026-03-20");
        feedback.setCreatedAt(createdAt);
        feedback.setCreatedDate(createdDate);

        // When
        feedback.prePersist();

        // Then
        assertThat(feedback.getCreatedAt()).isEqualTo(createdAt);
        assertThat(feedback.getCreatedDate()).isEqualTo(createdDate);
    }

    @Test
    void givenOnlyCreatedAtPresent_whenPrePersist_thenInitializeOnlyCreatedDate() {
        // Given
        Feedback feedback = new Feedback();
        Instant createdAt = Instant.parse("2026-03-20T10:00:00Z");
        feedback.setCreatedAt(createdAt);

        // When
        feedback.prePersist();

        // Then
        assertThat(feedback.getCreatedAt()).isEqualTo(createdAt);
        assertThat(feedback.getCreatedDate()).isNotNull();
    }
}
