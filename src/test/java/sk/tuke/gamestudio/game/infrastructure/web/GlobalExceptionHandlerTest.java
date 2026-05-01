package sk.tuke.gamestudio.game.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import sk.tuke.gamestudio.game.domain.model.GameDomainException;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static Stream<GameDomainException> domainExceptions() {
        return Stream.of(
                new GameDomainException.NotFound("id"),
                new GameDomainException.Forbidden("id"),
                new GameDomainException.AlreadyWon(),
                new GameDomainException.MoveLimitReached(),
                new GameDomainException.InvalidColor("pink"),
                new GameDomainException.InvalidSize(0),
                new GameDomainException.StoreUnavailable()
        );
    }

    @ParameterizedTest
    @MethodSource("domainExceptions")
    void givenDomainException_whenHandle_thenMapToExpectedHttpStatus(GameDomainException ex) {
        var entity = handler.handle(ex);

        HttpStatus expected = ex instanceof GameDomainException.NotFound ? HttpStatus.NOT_FOUND
                : ex instanceof GameDomainException.Forbidden ? HttpStatus.FORBIDDEN
                : ex instanceof GameDomainException.StoreUnavailable ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.BAD_REQUEST;
        assertThat(entity.getStatusCode()).isEqualTo(expected);
        assertThat(entity.getBody().status()).isEqualTo("ERROR");
        assertThat(entity.getBody().error()).isEqualTo(ex.getMessage());
    }

    @Test
    void givenValidationException_whenHandleValidation_thenJoinFieldErrors() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(target, "obj");
        binding.addError(new FieldError("obj", "size", "must be >= 1"));
        binding.addError(new FieldError("obj", "color", "must not be blank"));

        Method method = this.getClass().getDeclaredMethod("dummy", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, binding);

        var entity = handler.handleValidation(ex);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody().error()).contains("size: must be >= 1", "color: must not be blank");
    }

    @Test
    void givenUnexpectedException_whenHandleUnexpected_thenReturnInternalServerError() {
        var entity = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(entity.getBody().error()).isEqualTo("Internal error");
    }

    @SuppressWarnings("unused")
    private void dummy(String arg) {
    }
}
