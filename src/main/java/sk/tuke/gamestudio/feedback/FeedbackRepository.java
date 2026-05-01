package sk.tuke.gamestudio.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import sk.tuke.gamestudio.feedback.model.Feedback;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Optional<Feedback> findFirstByAuthorEmailOrderByCreatedAtDesc(String authorEmail);
}
