package sk.tuke.gamestudio.authentication.core.service;

public interface EmailDeliveryService {
    void sendEmailVerification(String email, String username, String verificationToken);
    void sendPasswordReset(String email, String resetToken);
}
