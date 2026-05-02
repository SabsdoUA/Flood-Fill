package sk.tuke.gamestudio.authentication.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;
import sk.tuke.gamestudio.authentication.core.UserRepository;
import sk.tuke.gamestudio.authentication.core.model.AuthProvider;
import sk.tuke.gamestudio.authentication.core.model.User;

import javax.sql.DataSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock(answer = org.mockito.Answers.RETURNS_SELF)
    private HttpSecurity http;

    @Mock
    private ClientRegistrationRepository registrations;

    @Mock
    private DefaultSecurityFilterChain chain;

    @Mock
    private PersistentTokenBasedRememberMeServices rememberMeServices;

    @Mock
    private PersistentTokenRepository persistentTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DataSource dataSource;

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void givenHttpSecurity_whenBuildFilterChain_thenReturnBuiltChain() throws Exception {
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = config.securityFilterChain(http, registrations, rememberMeServices, persistentTokenRepository);

        assertThat(result).isSameAs(chain);
    }

    @Test
    void givenNestedSourceMapPath_whenEvaluateSourceMapMatchers_thenPathIsMatched() {
        RequestMatcher[] matchers = (RequestMatcher[]) ReflectionTestUtils.getField(SecurityConfig.class, "SOURCE_MAP_MATCHERS");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assets/js/app.js.map");
        request.setServletPath("/assets/js/app.js.map");

        assertThat(matchers).isNotNull();
        assertThat(matchers)
                .anySatisfy(matcher -> assertThat(matcher.matches(request)).isTrue());
    }

    @Test
    void givenPasswordEncoderBean_whenCreated_thenEncoderWorks() {
        PasswordEncoder encoder = config.passwordEncoder();

        String raw = "StrongPass1";
        String encoded = encoder.encode(raw);

        assertThat(encoded).isNotBlank().isNotEqualTo(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    @Test
    void givenCorsOrigins_whenBuildConfigurationSource_thenExposeExpectedPolicy() {
        ReflectionTestUtils.setField(config, "corsAllowedOrigins", List.of("http://localhost:5173", "https://example.com"));

        CorsConfigurationSource source = config.corsConfigurationSource();
        var cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/game/g1/start"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:5173", "https://example.com");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).contains("*");
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    @Test
    void givenUserRepository_whenBuildUserDetailsService_thenResolveLocalAndGoogleUsers() {
        User local = new User();
        local.setEmail("user@gmail.com");
        local.setPasswordHash("hash");
        local.setProvider(AuthProvider.LOCAL);
        local.setEmailVerified(true);

        User google = new User();
        google.setEmail("google@gmail.com");
        google.setProvider(AuthProvider.GOOGLE);
        google.setEmailVerified(false);

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(local));
        when(userRepository.findByEmail("google@gmail.com")).thenReturn(Optional.of(google));

        UserDetailsService service = config.userDetailsService(userRepository);

        assertThat(service.loadUserByUsername(" USER@GMAIL.COM ").getUsername()).isEqualTo("user@gmail.com");
        assertThat(service.loadUserByUsername("google@gmail.com").isEnabled()).isTrue();
    }

    @Test
    void givenUnverifiedLocalOrMissingUser_whenBuildUserDetailsService_thenDisableOrThrow() {
        User local = new User();
        local.setEmail("user@gmail.com");
        local.setPasswordHash("hash");
        local.setProvider(AuthProvider.LOCAL);
        local.setEmailVerified(false);

        when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.of(local));
        when(userRepository.findByEmail("missing@gmail.com")).thenReturn(Optional.empty());

        UserDetailsService service = config.userDetailsService(userRepository);

        assertThat(service.loadUserByUsername("user@gmail.com").isEnabled()).isFalse();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.loadUserByUsername("missing@gmail.com"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void givenRememberMeProperties_whenBuildRememberMeServices_thenApplyConfiguredValues() {
        ReflectionTestUtils.setField(config, "rememberMeKey", "secret-key");
        ReflectionTestUtils.setField(config, "rememberMeCookieName", "FF_REMEMBER_ME");
        ReflectionTestUtils.setField(config, "rememberMeTokenValiditySeconds", 1234);
        ReflectionTestUtils.setField(config, "rememberMeSecureCookie", true);
        ReflectionTestUtils.setField(config, "rememberMeAlwaysRemember", false);

        UserDetailsService service = username -> org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("hash")
                .authorities("ROLE_USER")
                .build();

        PersistentTokenBasedRememberMeServices rememberMeServices = config.rememberMeServices(service, persistentTokenRepository);

        assertThat(ReflectionTestUtils.getField(rememberMeServices, "key")).isEqualTo("secret-key");
        assertThat(ReflectionTestUtils.getField(rememberMeServices, "cookieName")).isEqualTo("FF_REMEMBER_ME");
        assertThat(ReflectionTestUtils.getField(rememberMeServices, "tokenValiditySeconds")).isEqualTo(1234);
        assertThat(ReflectionTestUtils.getField(rememberMeServices, "useSecureCookie")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(rememberMeServices, "alwaysRemember")).isEqualTo(false);
    }

    @Test
    void givenMissingRememberMeKey_whenBuildRememberMeServices_thenGenerateOne() {
        ReflectionTestUtils.setField(config, "rememberMeKey", "");
        ReflectionTestUtils.setField(config, "rememberMeCookieName", "FF_REMEMBER_ME");
        ReflectionTestUtils.setField(config, "rememberMeTokenValiditySeconds", 2592000);
        ReflectionTestUtils.setField(config, "rememberMeSecureCookie", false);
        ReflectionTestUtils.setField(config, "rememberMeAlwaysRemember", false);

        UserDetailsService service = username -> org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("hash")
                .authorities("ROLE_USER")
                .build();

        PersistentTokenBasedRememberMeServices rememberMeServices = config.rememberMeServices(service, persistentTokenRepository);

        assertThat(ReflectionTestUtils.getField(rememberMeServices, "key"))
                .asString()
                .isNotBlank();
    }

    @Test
    void givenDataSource_whenBuildPersistentTokenRepository_thenReturnJdbcRepository() {
        PersistentTokenRepository repository = config.persistentTokenRepository(dataSource);

        assertThat(repository).isInstanceOf(JdbcTokenRepositoryImpl.class);
    }
}
