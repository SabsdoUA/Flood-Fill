package sk.tuke.gamestudio.feedback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.authentication.core.UserRepository;
import sk.tuke.gamestudio.authentication.core.model.User;
import sk.tuke.gamestudio.feedback.dto.CreateFeedbackRequest;
import sk.tuke.gamestudio.feedback.dto.FeedbackResponse;
import sk.tuke.gamestudio.feedback.model.Feedback;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void givenUnknownUser_whenCreateFeedback_thenThrowUnauthorized() {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest(5, "Great game");
        when(userRepository.findByEmail("missing@user.com")).thenReturn(Optional.empty());

        // When / Then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> feedbackService.createFeedback("missing@user.com", request)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void givenRecentFeedbackWithinCooldown_whenCreateFeedback_thenThrowTooManyRequests() {
        // Given
        User user = user("player@game.com", "Player One", "");
        CreateFeedbackRequest request = new CreateFeedbackRequest(4, "Good");
        Feedback recent = feedback("Player One", "player@game.com", 5, "Prev",
                Instant.now().minusSeconds(60), LocalDate.now());

        when(userRepository.findByEmail("player@game.com")).thenReturn(Optional.of(user));
        when(feedbackRepository.findFirstByAuthorEmailOrderByCreatedAtDesc("player@game.com"))
                .thenReturn(Optional.of(recent));

        // When / Then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> feedbackService.createFeedback("player@game.com", request)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void givenPreviousFeedbackWithNullTimestamp_whenCreateFeedback_thenSaveAndNormalizeComment() {
        // Given
        User user = user("player@game.com", "  Name  ", "");
        CreateFeedbackRequest request = new CreateFeedbackRequest(3, "   ");
        Feedback previous = feedback("Old", "player@game.com", 2, "old", null, LocalDate.now());
        Feedback persisted = feedback("Name", "player@game.com", 3, null,
                Instant.parse("2026-03-29T10:15:30Z"), LocalDate.parse("2026-03-29"));
        persisted.setId(10L);

        when(userRepository.findByEmail("player@game.com")).thenReturn(Optional.of(user));
        when(feedbackRepository.findFirstByAuthorEmailOrderByCreatedAtDesc("player@game.com"))
                .thenReturn(Optional.of(previous));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(persisted);

        // When
        FeedbackResponse response = feedbackService.createFeedback("player@game.com", request);

        // Then
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.user()).isEqualTo("Name");
        assertThat(response.rating()).isEqualTo(3);
        assertThat(response.comment()).isNull();

        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void givenOldFeedbackOutsideCooldown_whenCreateFeedback_thenSaveAndMapResponse() {
        // Given
        User user = user("nick@game.com", "Ignored", "  Nick  ");
        CreateFeedbackRequest request = new CreateFeedbackRequest(5, "  excellent  ");
        Feedback old = feedback("Old", "nick@game.com", 1, "ancient",
                Instant.now().minusSeconds(60 * 60 * 25), LocalDate.now().minusDays(2));
        Feedback persisted = feedback("   ", "nick@game.com", 5, "excellent",
                Instant.parse("2026-03-28T07:00:00Z"), LocalDate.parse("2026-03-28"));
        persisted.setId(20L);

        when(userRepository.findByEmail("nick@game.com")).thenReturn(Optional.of(user));
        when(feedbackRepository.findFirstByAuthorEmailOrderByCreatedAtDesc("nick@game.com"))
                .thenReturn(Optional.of(old));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(persisted);

        // When
        FeedbackResponse response = feedbackService.createFeedback("nick@game.com", request);

        // Then
        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.user()).isEqualTo("Neznámy používateľ");
        assertThat(response.comment()).isEqualTo("excellent");
    }

    @Test
    void givenFeedbackList_whenGetAllFeedback_thenMapAndNormalizeNames() {
        // Given
        Feedback first = feedback(null, "a@a.com", 1, "x", Instant.parse("2026-01-01T01:00:00Z"), LocalDate.parse("2026-01-01"));
        first.setId(1L);
        Feedback second = feedback("  Alice  ", "b@b.com", 5, null, Instant.parse("2026-01-01T02:00:00Z"), LocalDate.parse("2026-01-01"));
        second.setId(2L);

        when(feedbackRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of(first, second));

        // When
        List<FeedbackResponse> responses = feedbackService.getAllFeedback();

        // Then
        assertThat(responses)
                .hasSize(2)
                .extracting(FeedbackResponse::user)
                .containsExactly("Neznámy používateľ", "Alice");
    }

    @Test
    void givenEmptyRepository_whenGetAllFeedback_thenReturnEmptyList() {
        // Given
        when(feedbackRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of());

        // When
        List<FeedbackResponse> responses = feedbackService.getAllFeedback();

        // Then
        assertThat(responses).isEmpty();
    }

    @Test
    void givenInvalidPageRequest_whenGetFeedback_thenClampToSafeBounds() {
        // Given
        when(feedbackRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(List.of());

        // When
        feedbackService.getFeedback(-5, 500);

        // Then
        verify(feedbackRepository).findAllByOrderByCreatedAtDesc(argThat(pageable ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 100));
    }

    private static User user(String email, String name, String nickname) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setNickname(nickname);
        return user;
    }

    private static Feedback feedback(String authorName, String authorEmail, int rating, String comment,
                                     Instant createdAt, LocalDate createdDate) {
        Feedback feedback = new Feedback();
        feedback.setAuthorName(authorName);
        feedback.setAuthorEmail(authorEmail);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setCreatedAt(createdAt);
        feedback.setCreatedDate(createdDate);
        return feedback;
    }
}
