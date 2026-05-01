package sk.tuke.gamestudio.authentication.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import sk.tuke.gamestudio.authentication.api.dto.LoginRequest;
import sk.tuke.gamestudio.authentication.api.dto.ForgotPasswordRequest;
import sk.tuke.gamestudio.authentication.api.dto.RegisterRequest;
import sk.tuke.gamestudio.authentication.api.dto.ResendVerificationRequest;
import sk.tuke.gamestudio.authentication.api.dto.ResetPasswordRequest;
import sk.tuke.gamestudio.authentication.core.model.User;
import sk.tuke.gamestudio.authentication.core.service.UserService;
import sk.tuke.gamestudio.authentication.infrastructure.AuthRateLimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UserService userService;
    @Mock private HttpServletRequest httpRequest;
    @Mock private HttpServletResponse httpResponse;
    @Mock private HttpSession session;
    @Mock private SessionRegistry sessionRegistry;
    @Mock private PersistentTokenBasedRememberMeServices rememberMeServices;
    @Mock private AuthRateLimiter authRateLimiter;

    @InjectMocks private AuthController controller;

    private static final String FRONTEND_URL = "https://flood-fill-896272314633.europe-west1.run.app";

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenRegisterRequest_whenRegister_thenDelegateToService() {
        RegisterRequest request = new RegisterRequest();
        when(userService.register(request)).thenReturn("Registrácia úspešná");

        String result = controller.register(request, httpRequest);

        assertThat(result).isEqualTo("Registrácia úspešná");
        verify(authRateLimiter).check(AuthRateLimiter.Bucket.REGISTER, httpRequest, request.getEmail());
        verify(userService).register(request);
    }

    @Test
    void givenLoginRequest_whenLogin_thenSetSecurityContextAndSession() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@gmail.com");
        User user = new User();
        user.setEmail("user@gmail.com");

        when(userService.login(request)).thenReturn(user);
        when(httpRequest.getSession(true)).thenReturn(session);

        String result = controller.login(request, httpRequest, httpResponse);

        assertThat(result).isEqualTo("Prihlásenie úspešné");
        verify(authRateLimiter).check(AuthRateLimiter.Bucket.LOGIN, httpRequest, request.getEmail());
        verify(session).setAttribute(eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
        verify(sessionRegistry).registerNewSession(session.getId(), "user@gmail.com");
        verify(rememberMeServices).loginSuccess(httpRequest, httpResponse, SecurityContextHolder.getContext().getAuthentication());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@gmail.com");
    }

    @Test
    void givenValidToken_whenVerifyEmail_thenRedirectToFrontendWithVerifiedOne() {
        ReflectionTestUtils.setField(controller, "frontendUrl", FRONTEND_URL);

        var response = controller.verifyEmail("valid-token");

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation()).hasToString(FRONTEND_URL + "/?verified=1");
        verify(userService).verifyEmail("valid-token");
    }

    @Test
    void givenInvalidToken_whenVerifyEmail_thenRedirectToFrontendWithVerifiedZero() {
        ReflectionTestUtils.setField(controller, "frontendUrl", FRONTEND_URL);
        doThrow(new RuntimeException("bad token")).when(userService).verifyEmail("bad-token");

        var response = controller.verifyEmail("bad-token");

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation()).hasToString(FRONTEND_URL + "/?verified=0");
        verify(userService).verifyEmail("bad-token");
    }

    @Test
    void givenEmail_whenResendVerification_thenDelegateAndReturnSuccessMessage() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("user@gmail.com");

        String result = controller.resendVerification(request, httpRequest);

        assertThat(result).isEqualTo("Overovací email bol odoslaný");
        verify(authRateLimiter).check(AuthRateLimiter.Bucket.RESEND_VERIFICATION, httpRequest, request.getEmail());
        verify(userService).resendVerification("user@gmail.com");
    }

    @Test
    void givenValidToken_whenValidateResetToken_thenReturnOk() {
        var response = controller.validateResetToken("reset-token", httpRequest);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(authRateLimiter).check(AuthRateLimiter.Bucket.VALIDATE_RESET_TOKEN, httpRequest, "reset-token");
        verify(userService).validateResetToken("reset-token");
    }

    @Test
    void givenEmail_whenForgotPassword_thenDelegateAndReturnNeutralMessage() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@gmail.com");

        String result = controller.forgotPassword(request, httpRequest);

        assertThat(result).isEqualTo("Ak účet existuje, pošleme vám email na obnovu hesla");
        verify(authRateLimiter).check(AuthRateLimiter.Bucket.FORGOT_PASSWORD, httpRequest, request.getEmail());
        verify(userService).forgotPassword("user@gmail.com");
    }

    @Test
    void givenTokenAndPassword_whenResetPassword_thenDelegateAndReturnSuccessMessage() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token-123");
        request.setNewPassword("NewStrongPass1!");

        String result = controller.resetPassword(request, httpRequest);

        assertThat(result).isEqualTo("Heslo bolo úspešne zmenené");
        verify(authRateLimiter).check(AuthRateLimiter.Bucket.RESET_PASSWORD, httpRequest, request.getToken());
        verify(userService).resetPassword("token-123", "NewStrongPass1!");
    }
}
