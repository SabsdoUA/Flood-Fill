package sk.tuke.gamestudio.authentication.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailConfigTest {

    @Test
    void givenDisplayNameFormat_whenNormalizeUsername_thenExtractEmailOnly() {
        String normalized = MailConfig.normalizeUsername("\"Flood Fill\" <user@gmail.com>");

        assertThat(normalized).isEqualTo("user@gmail.com");
    }

    @Test
    void givenGmailHostWithSpacedPassword_whenNormalizePassword_thenRemoveWhitespace() {
        String normalized = MailConfig.normalizePassword("smtp.gmail.com", "abcd efgh ijkl mnop");

        assertThat(normalized).isEqualTo("abcdefghijklmnop");
    }

    @Test
    void givenQuotedGmailPassword_whenNormalizePassword_thenStripQuotesAndWhitespace() {
        String normalized = MailConfig.normalizePassword("smtp.gmail.com", "\"abcd efgh ijkl mnop\"");

        assertThat(normalized).isEqualTo("abcdefghijklmnop");
    }

    @Test
    void givenNonGmailHost_whenNormalizePassword_thenKeepInternalWhitespace() {
        String normalized = MailConfig.normalizePassword("mail.private.local", "a b c");

        assertThat(normalized).isEqualTo("a b c");
    }
}
