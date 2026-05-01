package sk.tuke.gamestudio.authentication.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimiterTest {

    @Test
    void givenLoginLimitExceededForSameIpAndIdentity_whenCheck_thenThrowTooManyRequests() {
        AuthRateLimiter limiter = new AuthRateLimiter(
                Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC)
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        for (int i = 0; i < 10; i++) {
            limiter.check(AuthRateLimiter.Bucket.LOGIN, request, " USER@GMAIL.COM ");
        }

        assertThatThrownBy(() -> limiter.check(AuthRateLimiter.Bucket.LOGIN, request, "user@gmail.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("429");
    }

    @Test
    void givenForwardedForHeader_whenCheck_thenUseFirstForwardedIp() {
        AuthRateLimiter limiter = new AuthRateLimiter(
                Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC)
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");

        for (int i = 0; i < 5; i++) {
            limiter.check(AuthRateLimiter.Bucket.REGISTER, request, "user@gmail.com");
        }

        assertThatThrownBy(() -> limiter.check(AuthRateLimiter.Bucket.REGISTER, request, "user@gmail.com"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
