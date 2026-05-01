package sk.tuke.gamestudio.feedback;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.authentication.core.UserRepository;
import sk.tuke.gamestudio.authentication.core.model.User;
import sk.tuke.gamestudio.feedback.dto.CreateFeedbackRequest;
import sk.tuke.gamestudio.feedback.dto.FeedbackResponse;
import sk.tuke.gamestudio.feedback.model.Feedback;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final Duration COMMENT_COOLDOWN = Duration.ofHours(24);
    private static final int DEFAULT_FEEDBACK_PAGE_SIZE = 50;
    private static final int MAX_FEEDBACK_PAGE_SIZE = 100;

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public FeedbackResponse createFeedback(String userEmail, CreateFeedbackRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Používateľ nie je prihlásený"));

        enforceCooldown(user.getEmail());

        Feedback feedback = new Feedback();
        feedback.setAuthorName(user.displayName());
        feedback.setUser(user);
        feedback.setAuthorEmail(user.getEmail());
        feedback.setRating(request.rating());
        feedback.setComment(normalizeComment(request.comment()));

        Feedback saved = feedbackRepository.save(feedback);
        return toResponse(saved);
    }

    public List<FeedbackResponse> getAllFeedback() {
        return getFeedback(0, DEFAULT_FEEDBACK_PAGE_SIZE);
    }

    public List<FeedbackResponse> getFeedback(int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_FEEDBACK_PAGE_SIZE);

        return feedbackRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(normalizedPage, normalizedSize)).stream()
                .map(this::toResponse)
                .toList();
    }


    private void enforceCooldown(String email) {
        Instant threshold = Instant.now().minus(COMMENT_COOLDOWN);
        feedbackRepository.findFirstByAuthorEmailOrderByCreatedAtDesc(email)
                .filter(previous -> previous.getCreatedAt() != null && previous.getCreatedAt().isAfter(threshold))
                .ifPresent(previous -> {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Komentár môžete pridať iba raz za 24 hodín");
                });
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                normalizeUserName(feedback.getAuthorName()),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt(),
                feedback.getCreatedDate()
        );
    }

    private String normalizeUserName(String userName) {
        return java.util.Optional.ofNullable(userName)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .orElse("Neznámy používateľ");
    }

    private String normalizeComment(String comment) {
        return java.util.Optional.ofNullable(comment)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .orElse(null);
    }
}
