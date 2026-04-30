package sk.tuke.gamestudio.authentication.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.authentication.core.service.EmailDeliveryException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void givenValidationException_whenHandleValidation_thenReturnFieldMap() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "email", "invalid"));
        binding.addError(new FieldError("obj", "password", "too short"));

        Method method = this.getClass().getDeclaredMethod("dummy", String.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), binding);

        var entity = handler.handleValidation(ex);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody()).containsKey("errors");
    }

    @Test
    void givenDataIntegrityViolation_whenHandled_thenReturnBadRequestAndMessage() {
        var entity = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("dup"));

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody().get("error").toString()).contains("Nepodarilo sa uložiť komentár");
    }

    @Test
    void givenResponseStatusWithReason_whenHandled_thenUseReason() {
        var entity = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.CONFLICT, "Conflict text"));

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(entity.getBody().get("error")).isEqualTo("Conflict text");
    }

    @Test
    void givenResponseStatusWithoutReason_whenHandled_thenUseDefaultMessage() {
        var entity = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.BAD_REQUEST));

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(entity.getBody().get("error")).isEqualTo("Požiadavka zlyhala");
    }


    @Test
    void givenEmailDeliveryException_whenHandled_thenReturnServiceUnavailable() {
        var entity = handler.handleEmailDelivery(new EmailDeliveryException("mail unavailable"));

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(entity.getBody().get("error")).isEqualTo("mail unavailable");
    }

    @SuppressWarnings("unused")
    private void dummy(String arg) {}
}
