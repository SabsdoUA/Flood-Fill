package sk.tuke.gamestudio.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalApiExceptionHandlerTest {

    private final GlobalApiExceptionHandler handler = new GlobalApiExceptionHandler();

    @Test
    void givenValidationError_whenHandleValidation_thenReturnBadRequestWithDetails() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "email", null));
        binding.addError(new FieldError("obj", "email", "should be ignored duplicate"));
        binding.addError(new FieldError("obj", "password", "too short"));

        Method method = this.getClass().getDeclaredMethod("dummy", String.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new MethodParameter(method, 0), binding);

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKeys("timestamp", "error", "details");
        assertThat(response.getBody().get("error")).isEqualTo("Validation failed");

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertThat(details).containsEntry("email", "invalid");
        assertThat(details).containsEntry("password", "too short");
    }

    @Test
    void givenUnexpectedException_whenHandleUnexpected_thenReturnInternalServerError() {
        var response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsKeys("timestamp", "error");
        assertThat(response.getBody().get("error")).isEqualTo("Internal server error");
    }

    @Test
    void givenNoResourceFound_whenHandleNotFound_thenReturnNotFound() {
        var response = handler.handleNotFound(new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKeys("timestamp", "error");
        assertThat(response.getBody().get("error")).isEqualTo("Not found");
    }

    @SuppressWarnings("unused")
    private void dummy(String arg) {}
}
