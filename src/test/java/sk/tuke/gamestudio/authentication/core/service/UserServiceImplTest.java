package sk.tuke.gamestudio.authentication.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.authentication.api.dto.LoginRequest;
import sk.tuke.gamestudio.authentication.api.dto.RegisterRequest;
import sk.tuke.gamestudio.authentication.core.UserRepository;
import sk.tuke.gamestudio.authentication.core.model.AuthProvider;
import sk.tuke.gamestudio.authentication.core.model.User;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailDeliveryService emailDeliveryService;
    @Mock private SessionRegistry sessionRegistry;
    @Mock private PersistentTokenRepository persistentTokenRepository;

    @InjectMocks private UserServiceImpl service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenOAuthTokenWithEmailAndExistingUser_whenResolveOAuthUser_thenReturnDisplayName() {
        User existing = user("user@gmail.com", "Nick", null, AuthProvider.GOOGLE, null);
        SecurityContextHolder.getContext().setAuthentication(oauthToken(Map.of("email", "user@gmail.com", "name", "Name")));
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(existing));

        String result = service.resolveOAuthUser();

        assertThat(result).isEqualTo("Prihlásený používateľ: Nick");
    }

    @Test
    void givenOAuthTokenWithNewEmail_whenResolveOAuthUser_thenCreateGoogleUser() {
        SecurityContextHolder.getContext().setAuthentication(oauthToken(Map.of("email", "new@gmail.com", "name", "New User")));
        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        String result = service.resolveOAuthUser();

        assertThat(result).isEqualTo("Prihlásený používateľ: New User");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void givenOAuthWithoutEmail_whenResolveOAuthUser_thenThrowUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(oauthToken(Map.of("name", "Only Name")));

        assertThatThrownBy(() -> service.resolveOAuthUser())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void givenNonOauthPrincipal_whenResolveOAuthUser_thenReturnAnonymousMessage() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user@gmail.com", "x"));

        assertThat(service.resolveOAuthUser()).isEqualTo("Neprihlásený používateľ");
    }

    @Test
    void givenNewLocalUser_whenRegister_thenSaveAndSendVerificationEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(" USER@GMAIL.COM ");
        req.setNickname(" Nick ");
        req.setPassword("StrongPass1");

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByNickname("Nick")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("hash");

        String result = service.register(req);

        assertThat(result).startsWith("Registrácia úspešná");
        verify(userRepository).save(any(User.class));
        verify(emailDeliveryService).sendEmailVerification(eq("user@gmail.com"), eq("Nick"), any(String.class));
    }

    @Test
    void givenExistingGoogleEmail_whenRegister_thenThrowConflictWithGoogleMessage() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@gmail.com");
        req.setNickname("Nick");
        req.setPassword("StrongPass1");

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user("user@gmail.com", null, null, AuthProvider.GOOGLE, null)));

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).isEqualTo("Email už existuje cez Google");
                });
    }

    @Test
    void givenExistingNickname_whenRegister_thenThrowConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@gmail.com");
        req.setNickname("Nick");
        req.setPassword("StrongPass1");

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.existsByNickname("Nick")).thenReturn(true);

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void givenVerifiedLocalUserAndMatchingPassword_whenLogin_thenReturnUser() {
        LoginRequest req = new LoginRequest();
        req.setEmail(" USER@GMAIL.COM ");
        req.setPassword("StrongPass1");
        User user = user("user@gmail.com", "Nick", null, AuthProvider.LOCAL, "hash");
        user.setEmailVerified(true);

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", "hash")).thenReturn(true);

        User result = service.login(req);

        assertThat(result).isSameAs(user);
    }

    @Test
    void givenUnverifiedLocalUser_whenLogin_thenThrowForbidden() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("StrongPass1");
        User user = user("user@gmail.com", "Nick", null, AuthProvider.LOCAL, "hash");
        user.setEmailVerified(false);

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void givenMissingUser_whenLogin_thenThrowUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("StrongPass1");
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void givenGoogleUser_whenLogin_thenThrowConflict() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("StrongPass1");
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(user("user@gmail.com", null, null, AuthProvider.GOOGLE, null)));

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void givenWrongPasswordOrNullHash_whenLogin_thenThrowUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("StrongPass1");

        User nullHash = user("user@gmail.com", null, null, AuthProvider.LOCAL, null);
        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(nullHash));

        assertThatThrownBy(() -> service.login(req)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void givenNoAuthentication_whenCurrentUser_thenReturnNull() {
        assertThat(service.currentUser()).isNull();
    }

    @Test
    void givenAuthenticationName_whenCurrentUser_thenReturnNormalizedUserContext() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(" USER@GMAIL.COM ", "x"));

        UserService.UserContext ctx = service.currentUser();

        assertThat(ctx).isNotNull();
        assertThat(ctx.email()).isEqualTo("user@gmail.com");
    }

    @Test
    void givenNullContext_whenResolveUserContext_thenEmpty() {
        assertThat(service.resolveUser((UserService.UserContext) null)).isEmpty();
    }

    @Test
    void givenContextWithEmailAndNicknameFallback_whenResolveUserContext_thenResolveByNickname() {
        UserService.UserContext ctx = new UserService.UserContext(" Nick ");
        User user = user("mail@gmail.com", "Nick", null, AuthProvider.LOCAL, null);

        when(userRepository.findByEmail("nick")).thenReturn(Optional.empty());
        when(userRepository.findByNickname("nick")).thenReturn(Optional.of(user));

        assertThat(service.resolveUser(ctx)).contains(user);
    }

    @Test
    void givenPrincipalNull_whenResolveUserPrincipal_thenEmpty() {
        assertThat(service.resolveUser((Principal) null)).isEmpty();
    }

    @Test
    void givenPrincipalWithEmail_whenResolveIdentity_thenReturnNormalizedIdentity() {
        Principal principal = () -> " USER@GMAIL.COM ";

        assertThat(service.resolveIdentity(principal)).contains("user@gmail.com");
    }

    @Test
    void givenOAuthPrincipalWithPreferredUsername_whenResolveUserPrincipal_thenResolveByNickname() {
        OAuth2AuthenticationToken token = oauthToken(Map.of("preferred_username", "Nick"));
        User user = user("mail@gmail.com", "Nick", null, AuthProvider.LOCAL, null);

        when(userRepository.findByEmail("nick")).thenReturn(Optional.empty());
        when(userRepository.findByNickname("nick")).thenReturn(Optional.of(user));

        assertThat(service.resolveUser((Principal) token)).contains(user);
    }

    @Test
    void givenValidResetToken_whenResetPassword_thenRevokeRememberMeTokens() {
        User user = user("user@gmail.com", "Nick", null, AuthProvider.LOCAL, "old-hash");
        user.setResetToken("token");
        user.setResetTokenExpiresAt(java.time.Instant.now().plusSeconds(60));

        when(userRepository.findByResetToken("token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewStrongPass1!")).thenReturn("new-hash");

        service.resetPassword("token", "NewStrongPass1!");

        verify(persistentTokenRepository).removeUserTokens("user@gmail.com");
    }

    private static OAuth2AuthenticationToken oauthToken(Map<String, Object> attrs) {
        String nameAttributeKey = attrs.containsKey("email")
                ? "email"
                : attrs.containsKey("preferred_username")
                ? "preferred_username"
                : attrs.containsKey("name")
                ? "name"
                : "sub";

        OAuth2User principal = new DefaultOAuth2User(List.of(), attrs, nameAttributeKey);
        return new OAuth2AuthenticationToken(principal, List.of(), "google");
    }

    private static User user(String email, String nickname, String name, AuthProvider provider, String hash) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setName(name);
        user.setProvider(provider);
        user.setPasswordHash(hash);
        user.setEmailVerified(true);
        return user;
    }
}
