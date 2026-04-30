package sk.tuke.gamestudio.feedback.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackResponseTest {

    @Test
    void givenSameValues_whenCompareRecords_thenUseValueSemantics() {
        Instant createdAt = Instant.parse("2026-04-27T10:15:30Z");
        LocalDate createdDate = LocalDate.of(2026, 4, 27);
        FeedbackResponse left = new FeedbackResponse(1L, "nick", 5, "ok", createdAt, createdDate);
        FeedbackResponse right = new FeedbackResponse(1L, "nick", 5, "ok", createdAt, createdDate);

        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
        assertThat(left.toString()).contains("nick", "rating=5", "comment=ok");
    }
}
