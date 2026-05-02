package sk.tuke.gamestudio.authentication.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import sk.tuke.gamestudio.authentication.core.UserRepository;
import sk.tuke.gamestudio.authentication.core.model.AuthProvider;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String OAUTH2_BASE_URI =
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;

    private static final String[] PUBLIC_ENDPOINTS =
            { "/", "/secured", "/auth/register", "/auth/login",
              "/auth/verify-email", "/auth/resend-verification",
              "/auth/forgot-password", "/auth/validate-reset-token", "/auth/reset-password",
              "/api/leaderboard/**", "/api/feedback/**" };

    private static final String[] PUBLIC_HEALTH_ENDPOINTS =
            { "/actuator/health", "/actuator/health/**" };

    static final RequestMatcher[] SOURCE_MAP_MATCHERS = {
            new AntPathRequestMatcher("/*.map"),
            new AntPathRequestMatcher("/assets/**/*.map")
    };

    // Static frontend assets must be public — served from Spring Boot on Cloud Run
    private static final String[] STATIC_RESOURCE_PATTERNS =
            { "/*.html", "/*.js", "/*.css", "/*.ico", "/*.svg",
              "/*.png", "/*.woff", "/*.woff2", "/*.ttf", "/assets/**" };

    @Value("${app.cors-allowed-origins}")
    private List<String> corsAllowedOrigins;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.auth.remember-me.key:}")
    private String rememberMeKey;

    @Value("${app.auth.remember-me.cookie-name:FF_REMEMBER_ME}")
    private String rememberMeCookieName;

    @Value("${app.auth.remember-me.token-validity-seconds:2592000}")
    private int rememberMeTokenValiditySeconds;

    @Value("${app.auth.remember-me.secure-cookie:false}")
    private boolean rememberMeSecureCookie;

    @Value("${app.auth.remember-me.always-remember:false}")
    private boolean rememberMeAlwaysRemember;

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByEmail(normalizeIdentity(username))
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash() == null ? "N/A" : user.getPasswordHash())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                        .disabled(!isLoginEnabled(user))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    public PersistentTokenBasedRememberMeServices rememberMeServices(
            UserDetailsService userDetailsService,
            PersistentTokenRepository persistentTokenRepository
    ) {
        String effectiveRememberMeKey = resolveRememberMeKey();
        var services = new PersistentTokenBasedRememberMeServices(
                effectiveRememberMeKey,
                userDetailsService,
                persistentTokenRepository
        );
        services.setCookieName(rememberMeCookieName);
        services.setParameter("remember-me");
        services.setAlwaysRemember(rememberMeAlwaysRemember);
        services.setTokenValiditySeconds(rememberMeTokenValiditySeconds);
        services.setUseSecureCookie(rememberMeSecureCookie);
        return services;
    }

    private String resolveRememberMeKey() {
        if (StringUtils.hasText(rememberMeKey)) {
            return rememberMeKey;
        }

        byte[] generatedKey = new byte[32];
        SECURE_RANDOM.nextBytes(generatedKey);
        rememberMeKey = Base64.getUrlEncoder().withoutPadding().encodeToString(generatedKey);
        log.warn("APP_REMEMBER_ME_KEY is not configured. Generated an in-memory remember-me key; existing remember-me sessions will not survive restart.");
        return rememberMeKey;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository,
            PersistentTokenBasedRememberMeServices rememberMeServices,
            PersistentTokenRepository persistentTokenRepository
    ) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");

        var defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, OAUTH2_BASE_URI);
        OAuth2AuthorizationRequestResolver resolver = new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                OAuth2AuthorizationRequest requestBase = defaultResolver.resolve(request);
                String registrationId = extractRegistrationId(request);
                return customize(requestBase, request, registrationId);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                OAuth2AuthorizationRequest requestBase = defaultResolver.resolve(request, clientRegistrationId);
                return customize(requestBase, request, clientRegistrationId);
            }
        };

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/game/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/feedback", "/api/leaderboard/win").authenticated()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(PUBLIC_HEALTH_ENDPOINTS).permitAll()
                        .requestMatchers(SOURCE_MAP_MATCHERS).denyAll()
                        .requestMatchers(STATIC_RESOURCE_PATTERNS).permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(-1)
                                .sessionRegistry(sessionRegistry())))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(resolver))
                        .defaultSuccessUrl("/secured", true)
                        .failureHandler((request, response, exception) ->
                                handleOAuthFailure(request, response, exception)))
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeServices(rememberMeServices))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", rememberMeCookieName, "XSRF-TOKEN")
                        .addLogoutHandler((request, response, authentication) -> {
                            if (authentication != null) {
                                persistentTokenRepository.removeUserTokens(normalizeIdentity(authentication.getName()));
                            }
                        })
                        .addLogoutHandler(rememberMeServices)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)
                .build();
    }

    private OAuth2AuthorizationRequest customize(
            OAuth2AuthorizationRequest baseRequest,
            HttpServletRequest request,
            String registrationId
    ) {
        if (baseRequest == null) {
            return null;
        }

        return OAuth2AuthorizationRequest.from(baseRequest)
                .additionalParameters(params -> params.put("prompt", "select_account"))
                .build();
    }

    private String extractRegistrationId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int slash = uri.lastIndexOf('/');
        if (slash >= 0 && slash < uri.length() - 1) {
            return uri.substring(slash + 1);
        }
        return "google";
    }

    private String requestBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();

        if (host == null || host.isBlank()) {
            return "";
        }

        // Port 8080 is Cloud Run's internal container port; the public URL uses standard HTTPS (443)
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443)
                || ("https".equalsIgnoreCase(scheme) && port == 8080);
        if (defaultPort) {
            return scheme + "://" + host;
        }
        return scheme + "://" + host + ":" + port;
    }

    private void handleOAuthFailure(
            HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Exception exception
    ) throws IOException {
        log.warn("OAuth2 login failed: {}", exception.getMessage());
        response.sendRedirect(appendQueryParam(resolveFrontendUrl(request), "oauth=0"));
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

    private boolean isLocalAddress(String url) {
        String lower = url.toLowerCase();
        return lower.contains("localhost") || lower.contains("127.0.0.1");
    }

    private String appendQueryParam(String baseUrl, String param) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + param;
    }

    private static boolean isLoginEnabled(sk.tuke.gamestudio.authentication.core.model.User user) {
        return user.getProvider() == AuthProvider.GOOGLE || user.isEmailVerified();
    }

    private static String normalizeIdentity(String value) {
        return value == null ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            if (StringUtils.hasText(headerValue)) {
                return plain.resolveCsrfTokenValue(request, csrfToken);
            }
            return xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
