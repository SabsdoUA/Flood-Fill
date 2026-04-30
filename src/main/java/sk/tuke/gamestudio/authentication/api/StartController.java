package sk.tuke.gamestudio.authentication.api;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.tuke.gamestudio.authentication.core.model.User;
import sk.tuke.gamestudio.authentication.core.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StartController {

    private static final String OAUTH_SUCCESS_PARAM = "oauth=1";

    private final UserService userService;
    private final PersistentTokenBasedRememberMeServices rememberMeServices;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/secured")
    public ResponseEntity<Void> redirectAfterOAuth(HttpServletRequest request, HttpServletResponse response) {
        userService.resolveOAuthUser();
        userService.resolveUser(userService.currentUser()).ifPresent(user ->
                rememberMeServices.loginSuccess(request, response, rememberMeAuthentication(user)));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(appendQueryParam(resolveFrontendUrl(request), OAUTH_SUCCESS_PARAM)))
                .build();
    }

    @GetMapping("/secured/user")
    public String currentUser(Principal principal) {
        userService.resolveOAuthUser();
        return userService.resolveUser(principal)
                .or(() -> userService.resolveUser(userService.currentUser()))
                .map(user -> "Prihlásený používateľ: " + user.displayName())
                .orElse("Neprihlásený používateľ");
    }

    private String appendQueryParam(String baseUrl, String param) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + param;
    }

    private String resolveFrontendUrl(HttpServletRequest request) {
        String configured = normalizeConfiguredFrontendUrl(frontendUrl);
        String requestBaseUrl = requestBaseUrl(request);

        if (requestBaseUrl.isEmpty()) {
            return configured;
        }

        if (isLocalAddress(configured) && !isLocalAddress(requestBaseUrl)) {
            return requestBaseUrl;
        }
        return configured;
    }

    private String normalizeConfiguredFrontendUrl(String rawFrontendUrl) {
        String value = rawFrontendUrl == null ? "" : rawFrontendUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isEmpty() ? "http://localhost:5173" : value;
    }

    private String requestBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();

        if (host == null || host.isBlank()) {
            return "";
        }

        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443)
                || ("https".equalsIgnoreCase(scheme) && port == 8080);
        if (defaultPort) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }

    private boolean isLocalAddress(String url) {
        String lower = url.toLowerCase();
        return lower.contains("localhost") || lower.contains("127.0.0.1");
    }

    private static UsernamePasswordAuthenticationToken rememberMeAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
