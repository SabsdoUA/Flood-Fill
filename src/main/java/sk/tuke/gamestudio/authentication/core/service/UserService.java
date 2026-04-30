package sk.tuke.gamestudio.authentication.core.service;

import sk.tuke.gamestudio.authentication.api.dto.LoginRequest;
import sk.tuke.gamestudio.authentication.api.dto.RegisterRequest;
import sk.tuke.gamestudio.authentication.core.model.User;

import java.security.Principal;
import java.util.Optional;

public interface UserService {
    String resolveOAuthUser();
    String register(RegisterRequest request);
    User login(LoginRequest request);
    UserContext currentUser();
    Optional<String> resolveIdentity(Principal principal);
    Optional<User> resolveUser(UserContext ctx);
    Optional<User> resolveUser(Principal principal);
    void verifyEmail(String token);
    void resendVerification(String email);
    void forgotPassword(String email);
    void validateResetToken(String token);
    void resetPassword(String token, String newPassword);
    record UserContext(String email) {
    }
}
