package sk.tuke.gamestudio.game.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.stereotype.Component;
import sk.tuke.gamestudio.game.application.GameResponse;
import sk.tuke.gamestudio.game.domain.model.GameDomainException;

import static java.util.stream.Collectors.joining;

@Component("gameExceptionHandler")
@RestControllerAdvice(basePackages = "sk.tuke.gamestudio.game")
public class GlobalExceptionHandler {

    @ExceptionHandler(GameDomainException.class)
    public ResponseEntity<GameResponse> handle(GameDomainException ex) {
        var status = switch (ex) {
            case GameDomainException.NotFound          e -> HttpStatus.NOT_FOUND;
            case GameDomainException.Forbidden         e -> HttpStatus.FORBIDDEN;
            case GameDomainException.AlreadyWon        e -> HttpStatus.BAD_REQUEST;
            case GameDomainException.MoveLimitReached  e -> HttpStatus.BAD_REQUEST;
            case GameDomainException.InvalidColor      e -> HttpStatus.BAD_REQUEST;
            case GameDomainException.InvalidSize       e -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GameResponse> handleValidation(MethodArgumentNotValidException ex) {
        var msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(joining(", "));
        return ResponseEntity.badRequest().body(error(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GameResponse> handleUnexpected(Exception ignored) {
        return ResponseEntity.internalServerError().body(error("Internal error"));
    }

    private static GameResponse error(String msg) {
        return new GameResponse(null, null, 0, 0, "ERROR", false, msg);
    }
}
