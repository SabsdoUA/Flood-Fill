package sk.tuke.gamestudio.authentication.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Locale;
import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host:smtp.gmail.com}") String host,
            @Value("${spring.mail.port:587}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean smtpAuth,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean startTlsEnabled
    ) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(trimToEmpty(host));
        sender.setPort(port);
        sender.setUsername(normalizeUsername(username));
        sender.setPassword(normalizePassword(host, password));

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTlsEnabled));

        return sender;
    }

    static String normalizeUsername(String rawUsername) {
        String username = trimToEmpty(rawUsername);
        if (username.startsWith("\"") && username.endsWith("\"") && username.length() > 1) {
            username = username.substring(1, username.length() - 1).trim();
        } else if (username.startsWith("'") && username.endsWith("'") && username.length() > 1) {
            username = username.substring(1, username.length() - 1).trim();
        }

        int lt = username.indexOf('<');
        int gt = username.indexOf('>');
        if (lt >= 0 && gt > lt + 1) {
            username = username.substring(lt + 1, gt).trim();
        }
        return username;
    }

    static String normalizePassword(String host, String rawPassword) {
        String password = trimToEmpty(rawPassword);
        if (password.startsWith("\"") && password.endsWith("\"") && password.length() > 1) {
            password = password.substring(1, password.length() - 1).trim();
        } else if (password.startsWith("'") && password.endsWith("'") && password.length() > 1) {
            password = password.substring(1, password.length() - 1).trim();
        }
        String normalizedHost = trimToEmpty(host).toLowerCase(Locale.ROOT);

        if (normalizedHost.contains("gmail.com")) {
            return password.replaceAll("\\s+", "");
        }
        return password;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
