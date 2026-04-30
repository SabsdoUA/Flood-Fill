package sk.tuke.gamestudio.feedback.dto;

import java.time.Instant;
import java.time.LocalDate;

public record FeedbackResponse(
        Long id,
        String user,
        int rating,
        String comment,
        Instant createdAt,
        LocalDate createdDate
) {}
