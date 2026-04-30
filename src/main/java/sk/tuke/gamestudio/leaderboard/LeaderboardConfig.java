package sk.tuke.gamestudio.leaderboard;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableAsync
@EnableCaching
@EnableRetry(order = 200)
@EnableTransactionManagement(order = 300)
@RequiredArgsConstructor
public class LeaderboardConfig {

    private final LeaderboardRepository leaderboardRepository;
    private final TransactionTemplate transactionTemplate;

    // ── Cache ─────────────────────────────────────────────────────────────────
    @Bean
    public CacheManager cacheManager() {
        var manager = new CaffeineCacheManager("leaderboard");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .recordStats());
        return manager;
    }

    // ── Inicializácia dát ─────────────────────────────────────────────────────
    @Bean
    public ApplicationRunner leaderboardInitializer() {
        return args -> {
            var startedAt = System.currentTimeMillis();
            log.info("UserStats sync started");

            try {
                var processed = transactionTemplate.execute(
                        status -> leaderboardRepository.insertMissingStats()
                );
                var durationMs = System.currentTimeMillis() - startedAt;
                log.info("UserStats sync finished: {} entries synced in {} ms", processed == null ? 0 : processed, durationMs);
            } catch (Exception ex) {
                log.error("UserStats sync failed during startup. Application will continue to serve HTTP. Cause: {}", ex.getMessage(), ex);
            }
        };
    }
}
