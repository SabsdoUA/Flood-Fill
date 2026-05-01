package sk.tuke.gamestudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class FloodFillApplication {
    private static final Logger log = LoggerFactory.getLogger(FloodFillApplication.class);

    static {
        System.setProperty("io.netty.noUnsafe", System.getProperty("io.netty.noUnsafe", "true"));
    }

    @Bean
    ApplicationListener<ApplicationStartedEvent> startupConfigLogger(Environment environment) {
        return event -> {
            var port = environment.getProperty("server.port", environment.getProperty("PORT", "8080"));
            var host = environment.getProperty("server.address", "0.0.0.0");
            var profile = String.join(",", environment.getActiveProfiles());
            if (profile.isBlank()) {
                profile = "default";
            }
            log.info("Startup config initialized: profiles={}, host={}, port={}", profile, host, port);
        };
    }

    @Bean
    ApplicationListener<ApplicationFailedEvent> startupFailureLogger() {
        return event -> log.error("Application startup failed before readiness: {}", event.getException().getMessage(), event.getException());
    }

    @Bean
    org.springframework.boot.CommandLineRunner checkRedis(StringRedisTemplate redisTemplate, Environment environment) {
        return args -> {
            try {
                String pong = redisTemplate.getConnectionFactory()
                        .getConnection()
                        .ping();
                log.info("Redis ping result: {}", pong);
            } catch (Exception e) {
                boolean failFast = environment.getProperty("app.redis.fail-fast", Boolean.class, false);
                if (failFast) {
                    throw new IllegalStateException("Redis ping failed during startup", e);
                }
                log.warn("Redis ping failed during startup (non-fatal): {}", e.getMessage());
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(FloodFillApplication.class, args);
    }
}
