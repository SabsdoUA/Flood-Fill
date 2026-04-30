package sk.tuke.gamestudio.leaderboard;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "sk.tuke.gamestudio.leaderboard")
public class LeaderboardExceptionHandler {

    // ── Výnimka ───────────────────────────────────────────────────────────────
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public static class WinRecordingException extends RuntimeException {
        public WinRecordingException(String message) { super(message); }
        public WinRecordingException(String message, Throwable cause) { super(message, cause); }
    }

    // ── 400 ───────────────────────────────────────────────────────────────────
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        log.debug("Bad leaderboard request: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid request parameters");
        return problem;
    }

    // ── 503 ───────────────────────────────────────────────────────────────────
    @ExceptionHandler(WinRecordingException.class)
    public ProblemDetail handleWinRecording(WinRecordingException ex) {
        log.error("Win recording permanently failed: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Could not record win due to high contention. Please retry."
        );
        problem.setTitle("Win recording temporarily unavailable");
        return problem;
    }
}