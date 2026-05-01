package sk.tuke.gamestudio.authentication.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuthRateLimiter {

    public enum Bucket {
        REGISTER(5, Duration.ofHours(1)),
        LOGIN(10, Duration.ofMinutes(15)),
        RESEND_VERIFICATION(5, Duration.ofHours(1)),
        FORGOT_PASSWORD(5, Duration.ofHours(1)),
        VALIDATE_RESET_TOKEN(30, Duration.ofMinutes(15)),
        RESET_PASSWORD(10, Duration.ofHours(1));

        private final int limit;
        private final Duration window;

        Bucket(int limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }
    }

    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimiter() {
        this(Clock.systemUTC());
    }

    AuthRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(Bucket bucket, HttpServletRequest request, String identity) {
        String key = bucket.name() + ':' + clientIp(request) + ':' + normalize(identity);
        long now = clock.millis();

        Window window = windows.compute(key, (ignored, existing) -> {
            if (existing == null || now >= existing.resetAtMillis) {
                return new Window(now + bucket.window.toMillis(), new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (window.count.get() > bucket.limit) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Príliš veľa pokusov. Skúste to neskôr."
            );
        }

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now >= entry.getValue().resetAtMillis);
        }
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int comma = forwardedFor.indexOf(',');
            return (comma >= 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static String normalize(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
    }

    private record Window(long resetAtMillis, AtomicInteger count) {
    }
}
