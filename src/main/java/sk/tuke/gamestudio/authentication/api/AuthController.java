package sk.tuke.gamestudio.authentication.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import sk.tuke.gamestudio.authentication.api.dto.ForgotPasswordRequest;
import sk.tuke.gamestudio.authentication.api.dto.LoginRequest;
import sk.tuke.gamestudio.authentication.api.dto.RegisterRequest;
import sk.tuke.gamestudio.authentication.api.dto.ResendVerificationRequest;
import sk.tuke.gamestudio.authentication.api.dto.ResetPasswordRequest;
import sk.tuke.gamestudio.authentication.core.service.UserService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SessionRegistry sessionRegistry;
    private final PersistentTokenBasedRememberMeServices rememberMeServices;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequest request,
                        HttpServletRequest httpRequest,
                        HttpServletResponse httpResponse) {
        var user = userService.login(request);

        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.getContext().setAuthentication(auth);

        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        sessionRegistry.registerNewSession(httpRequest.getSession(true).getId(), user.getEmail());
        rememberMeServices.loginSuccess(httpRequest, httpResponse, auth);

        return "Prihlásenie úspešné";
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        try {
            userService.verifyEmail(token);
            return ResponseEntity.status(302).location(URI.create(frontendUrl + "/?verified=1")).build();
        } catch (Exception e) {
            return ResponseEntity.status(302).location(URI.create(frontendUrl + "/?verified=0")).build();
        }
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        userService.resendVerification(request.getEmail());
        return "Overovací email bol odoslaný";
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<Void> validateResetToken(@RequestParam String token) {
        userService.validateResetToken(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return "Ak účet existuje, pošleme vám email na obnovu hesla";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return "Heslo bolo úspešne zmenené";
    }
}
