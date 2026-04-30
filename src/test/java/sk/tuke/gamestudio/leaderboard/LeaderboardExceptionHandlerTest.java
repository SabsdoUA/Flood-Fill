package sk.tuke.gamestudio.leaderboard;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import sk.tuke.gamestudio.leaderboard.LeaderboardExceptionHandler.WinRecordingException;

import static org.assertj.core.api.Assertions.assertThat;

class LeaderboardExceptionHandlerTest {

    private final LeaderboardExceptionHandler handler = new LeaderboardExceptionHandler();

    @Test
    void givenIllegalArgumentException_whenHandleBadRequest_thenBuild400Problem() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("invalid size");

        // When
        ProblemDetail problem = handler.handleBadRequest(ex);

        // Then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid request parameters");
        assertThat(problem.getDetail()).isEqualTo("invalid size");
    }

    @Test
    void givenConstraintViolationException_whenHandleBadRequest_thenBuild400Problem() {
        // Given
        ConstraintViolationException ex = new ConstraintViolationException("constraint failed", null);

        // When
        ProblemDetail problem = handler.handleBadRequest(ex);

        // Then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("constraint failed");
    }

    @Test
    void givenWinRecordingException_whenHandleWinRecording_thenBuild503Problem() {
        // Given
        WinRecordingException ex = new WinRecordingException("retry exhausted");

        // When
        ProblemDetail problem = handler.handleWinRecording(ex);

        // Then
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getTitle()).isEqualTo("Win recording temporarily unavailable");
        assertThat(problem.getDetail()).contains("Could not record win");
    }

    @Test
    void givenWinRecordingExceptionWithoutCause_whenConstructed_thenRetainMessage() {
        // Given / When
        WinRecordingException ex = new WinRecordingException("message only");

        // Then
        assertThat(ex.getMessage()).isEqualTo("message only");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void givenWinRecordingExceptionWithCause_whenConstructed_thenRetainCauseAndMessage() {
        // Given
        RuntimeException cause = new RuntimeException("root cause");

        // When
        WinRecordingException ex = new WinRecordingException("message", cause);

        // Then
        assertThat(ex.getMessage()).isEqualTo("message");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
