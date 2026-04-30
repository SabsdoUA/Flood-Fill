package sk.tuke.gamestudio.authentication.core.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingEmailDeliveryServiceTest {

    @Test
    void givenGmailAuthError_whenFallbackDisabled_thenThrowUserFriendlyAuthMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("535-5.7.8 Username and Password not accepted"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        LoggingEmailDeliveryService service = new LoggingEmailDeliveryService(
                mailSender,
                "",
                "http://localhost:8080",
                "",
                "sender@gmail.com",
                false
        );

        assertThatThrownBy(() -> service.sendEmailVerification("user@gmail.com", "TestUser", "token"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("SMTP autentifikácia zlyhala");
    }

    @Test
    void givenAuthError_whenFallbackEnabled_thenStillThrowUserFriendlyError() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("invalid credentials"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        LoggingEmailDeliveryService service = new LoggingEmailDeliveryService(
                mailSender,
                "",
                "http://localhost:8080",
                "",
                "sender@gmail.com",
                true
        );

        assertThatThrownBy(() -> service.sendEmailVerification("user@gmail.com", "TestUser", "token"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("SMTP autentifikácia zlyhala");
    }
}
