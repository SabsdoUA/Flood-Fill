package sk.tuke.gamestudio.authentication.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.authentication.api.dto.LoginRequest;
import sk.tuke.gamestudio.authentication.api.dto.RegisterRequest;
import sk.tuke.gamestudio.authentication.core.UserRepository;
import sk.tuke.gamestudio.authentication.core.model.AuthProvider;
import sk.tuke.gamestudio.authentication.core.model.User;

import java.security.Principal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailDeliveryService emailDeliveryService;
    private final SessionRegistry sessionRegistry;
    private final PersistentTokenRepository persistentTokenRepository;

    @Override
    @Transactional
    public String resolveOAuthUser() {
        return oauthAttributes()
                .map(this::persistOAuthUser)
                .map(User::displayName)
                .map(name -> "Prihlásený používateľ: " + name)
                .orElse("Neprihlásený používateľ");
    }

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        String email = normalize(request.getEmail());
        String nickname = request.getNickname().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw conflict(requestEmailConflictMessage(email));
        }
        if (userRepository.existsByNickname(nickname)) {
            throw conflict("Prezývka už existuje");
        }

        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setName(nickname);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);

        String token = newToken();
        user.setVerificationToken(tokenDigest(token));
        user.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        userRepository.save(user);
        emailDeliveryService.sendEmailVerification(email, nickname, token);

        return "Registrácia úspešná. Skontrolujte e-mail a potvrďte účet.";
    }

    @Override
    public User login(LoginRequest request) {
        String email = normalize(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> unauthorized("Neplatný email alebo heslo"));

        if (user.getProvider() == AuthProvider.GOOGLE) {
            throw conflict("Tento účet používa Google prihlásenie");
        }
        if (!passwordValid(user, request.getPassword())) {
            throw unauthorized("Neplatný email alebo heslo");
        }
        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Email nebol potvrdený. Skontrolujte vašu schránku alebo požiadajte o nový overovací email.");
        }
        return user;
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = findByVerificationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neplatný overovací token"));

        if (user.getVerificationTokenExpiresAt() != null
                && Instant.now().isAfter(user.getVerificationTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Overovací token vypršal");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        String normalized = normalize(email);
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Účet s týmto emailom neexistuje"));

        if (user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email je už potvrdený");
        }

        String token = newToken();
        user.setVerificationToken(tokenDigest(token));
        user.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        userRepository.save(user);
        emailDeliveryService.sendEmailVerification(normalized, user.getNickname(), token);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        String normalized = normalize(email);
        Optional<User> userOpt = userRepository.findByEmail(normalized);
        if (userOpt.isEmpty() || userOpt.get().getProvider() != AuthProvider.LOCAL) {
            return;
        }

        User user = userOpt.get();
        String token = newToken();
        user.setResetToken(tokenDigest(token));
        user.setResetTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        userRepository.save(user);
        emailDeliveryService.sendPasswordReset(normalized, token);
    }

    @Override
    public void validateResetToken(String token) {
        User user = findByResetToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neplatný token na obnovu hesla"));

        if (user.getResetTokenExpiresAt() != null
                && Instant.now().isAfter(user.getResetTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token na obnovu hesla vypršal");
        }
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = findByResetToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neplatný token na obnovu hesla"));

        if (user.getResetTokenExpiresAt() != null
                && Instant.now().isAfter(user.getResetTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token na obnovu hesla vypršal");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);

        invalidateUserSessions(user.getEmail());
        persistentTokenRepository.removeUserTokens(user.getEmail());
    }

    private void invalidateUserSessions(String email) {
        sessionRegistry.getAllSessions(email, false)
                .forEach(SessionInformation::expireNow);
    }

    @Override
    public UserContext currentUser() {
        return resolveIdentity(SecurityContextHolder.getContext().getAuthentication())
                .map(UserContext::new)
                .orElse(null);
    }

    @Override
    public Optional<String> resolveIdentity(Principal principal) {
        return resolvePrincipalIdentity(principal);
    }

    @Override
    public Optional<User> resolveUser(UserContext ctx) {
        return Optional.ofNullable(ctx)
                .map(UserContext::email)
                .map(this::normalize)
                .flatMap(this::findByIdentity);
    }

    @Override
    public Optional<User> resolveUser(Principal principal) {
        return resolvePrincipalIdentity(principal).flatMap(this::findByIdentity);
    }

    private Optional<Map<String, Object>> oauthAttributes() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(OAuth2AuthenticationToken.class::isInstance)
                .map(OAuth2AuthenticationToken.class::cast)
                .map(token -> token.getPrincipal().getAttributes());
    }

    private User persistOAuthUser(Map<String, Object> attrs) {
        String email = normalize((String) attrs.get("email"));
        if (email == null) {
            throw unauthorized("Neprihlásený používateľ");
        }
        return userRepository.findByEmail(email).orElseGet(() -> saveOAuthUser(email, attrs));
    }

    private User saveOAuthUser(String email, Map<String, Object> attrs) {
        User user = new User();
        user.setEmail(email);
        user.setName((String) attrs.getOrDefault("name", email));
        user.setProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private Optional<String> resolvePrincipalIdentity(Principal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        return Stream.<Supplier<Optional<String>>>of(
                        () -> principal instanceof OAuth2AuthenticationToken token
                                ? resolveOAuthIdentity(token)
                                : Optional.empty(),
                        () -> principal instanceof Authentication auth
                                ? normalizeOptional(auth.getName())
                                : Optional.empty(),
                        () -> normalizeOptional(principal.getName())
                )
                .map(Supplier::get)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<String> resolveOAuthIdentity(OAuth2AuthenticationToken token) {
        Map<String, Object> attrs = token.getPrincipal().getAttributes();
        return normalizeOptional((String) attrs.get("email"))
                .or(() -> normalizeOptional((String) attrs.get("preferred_username")));
    }

    private Optional<String> normalizeOptional(String value) {
        return Optional.ofNullable(normalize(value));
    }

    private Optional<User> findByIdentity(String identity) {
        return userRepository.findByEmail(identity)
                .or(() -> userRepository.findByNickname(identity));
    }

    private Optional<User> findByVerificationToken(String rawToken) {
        return userRepository.findByVerificationToken(tokenDigest(rawToken))
                .or(() -> userRepository.findByVerificationToken(rawToken));
    }

    private Optional<User> findByResetToken(String rawToken) {
        return userRepository.findByResetToken(tokenDigest(rawToken))
                .or(() -> userRepository.findByResetToken(rawToken));
    }

    private static String newToken() {
        return UUID.randomUUID().toString();
    }

    private static String tokenDigest(String rawToken) {
        if (rawToken == null) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private boolean passwordValid(User user, String rawPassword) {
        return user.getPasswordHash() != null && passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    private String requestEmailConflictMessage(String email) {
        return userRepository.findByEmail(email)
                .map(User::getProvider)
                .map(provider -> provider == AuthProvider.GOOGLE ? "Email už existuje cez Google" : "Email už existuje")
                .orElse("Email už existuje");
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
