package sk.tuke.gamestudio.authentication.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.test.util.ReflectionTestUtils;
import sk.tuke.gamestudio.authentication.core.model.User;
import sk.tuke.gamestudio.authentication.core.service.UserService;

import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartControllerTest {

    @Mock private UserService userService;
    @Mock private Principal principal;
    @Mock private PersistentTokenBasedRememberMeServices rememberMeServices;

    @InjectMocks private StartController controller;

    private static final String FRONTEND_URL = "https://flood-fill-896272314633.europe-west1.run.app";

    @Test
    void givenFrontendWithoutQuery_whenRedirectAfterOAuth_thenUseQuestionSeparator() {
        ReflectionTestUtils.setField(controller, "frontendUrl", FRONTEND_URL);
        when(userService.currentUser()).thenReturn(null);
        when(userService.resolveUser((UserService.UserContext) null)).thenReturn(Optional.empty());

        var response = controller.redirectAfterOAuth(
                mockRequest("https", "flood-fill-896272314633.europe-west1.run.app", 443),
                new MockHttpServletResponse()
        );

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation()).hasToString(FRONTEND_URL + "?oauth=1");
    }

    @Test
    void givenFrontendWithQuery_whenRedirectAfterOAuth_thenUseAmpersandSeparator() {
        ReflectionTestUtils.setField(controller, "frontendUrl", FRONTEND_URL + "?x=1");
        when(userService.currentUser()).thenReturn(null);
        when(userService.resolveUser((UserService.UserContext) null)).thenReturn(Optional.empty());

        var response = controller.redirectAfterOAuth(
                mockRequest("https", "flood-fill-896272314633.europe-west1.run.app", 443),
                new MockHttpServletResponse()
        );

        assertThat(response.getHeaders().getLocation()).hasToString(FRONTEND_URL + "?x=1&oauth=1");
    }

    @Test
    void givenLocalConfiguredFrontendOnCloudRun_whenRedirectAfterOAuth_thenUseRequestHost() {
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:5173");
        when(userService.currentUser()).thenReturn(null);
        when(userService.resolveUser((UserService.UserContext) null)).thenReturn(Optional.empty());

        var response = controller.redirectAfterOAuth(
                mockRequest("https", "flood-fill-896272314633.europe-west1.run.app", 443),
                new MockHttpServletResponse()
        );

        assertThat(response.getHeaders().getLocation())
                .hasToString("https://flood-fill-896272314633.europe-west1.run.app?oauth=1");
    }

    @Test
    void givenPrincipalUserResolved_whenCurrentUser_thenReturnDisplayName() {
        User user = new User();
        user.setNickname("nick");
        when(userService.resolveUser(principal)).thenReturn(Optional.of(user));
        when(userService.resolveOAuthUser()).thenReturn("ok");

        String result = controller.currentUser(principal);

        assertThat(result).isEqualTo("Prihlásený používateľ: nick");
    }

    @Test
    void givenPrincipalNotResolvedButCurrentUserResolved_whenCurrentUser_thenReturnDisplayNameFromFallback() {
        User user = new User();
        user.setName("Name");
        UserService.UserContext ctx = new UserService.UserContext("mail@gmail.com");

        when(userService.resolveOAuthUser()).thenReturn("ok");
        when(userService.resolveUser(principal)).thenReturn(Optional.empty());
        when(userService.currentUser()).thenReturn(ctx);
        when(userService.resolveUser(ctx)).thenReturn(Optional.of(user));

        String result = controller.currentUser(principal);

        assertThat(result).isEqualTo("Prihlásený používateľ: Name");
    }

    @Test
    void givenNoUserResolved_whenCurrentUser_thenReturnAnonymousMessage() {
        when(userService.resolveOAuthUser()).thenReturn("ok");
        when(userService.resolveUser(principal)).thenReturn(Optional.empty());
        when(userService.currentUser()).thenReturn(null);
        when(userService.resolveUser((UserService.UserContext) null)).thenReturn(Optional.empty());

        String result = controller.currentUser(principal);

        assertThat(result).isEqualTo("Neprihlásený používateľ");
    }

    private MockHttpServletRequest mockRequest(String scheme, String host, int port) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme(scheme);
        request.setServerName(host);
        request.setServerPort(port);
        request.setRequestURI("/secured");
        return request;
    }
}
