package sk.tuke.gamestudio.game.infrastructure.persistence;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import sk.tuke.gamestudio.game.domain.model.GameDomainException;
import sk.tuke.gamestudio.game.domain.port.Ports;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class RedisGameLock implements Ports.GameLock {

    private static final Logger log = LoggerFactory.getLogger(RedisGameLock.class);
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final Duration LOCK_WAIT = Duration.ofSeconds(2);
    private static final Duration RETRY_DELAY = Duration.ofMillis(25);
    private static final String KEY_PREFIX = "game-lock:";
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final RedisTemplate<String, String> redis;
    private final boolean localFallbackEnabled;
    private final Map<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    public RedisGameLock(
            @Qualifier("redisTemplate") RedisTemplate<String, String> redis,
            @Value("${app.game.local-state-fallback-enabled:true}") boolean localFallbackEnabled
    ) {
        this.redis = redis;
        this.localFallbackEnabled = localFallbackEnabled;
    }

    @Override
    public <T> T withLock(String gameId, Supplier<T> action) {
        String key = KEY_PREFIX + gameId;
        String token = UUID.randomUUID().toString();
        try {
            acquire(key, token);
        } catch (DataAccessException ex) {
            if (!localFallbackEnabled) {
                throw new GameDomainException.StoreUnavailable();
            }
            return withLocalLock(gameId, action);
        }

        try {
            return action.get();
        } finally {
            release(key, token);
        }
    }

    private void acquire(String key, String token) {
        long deadline = System.nanoTime() + LOCK_WAIT.toNanos();
        do {
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, LOCK_TTL))) {
                return;
            }
            sleepBeforeRetry();
        } while (System.nanoTime() < deadline);

        throw new GameDomainException.StoreUnavailable();
    }

    private void release(String key, String token) {
        try {
            redis.execute(RELEASE_SCRIPT, List.of(key), token);
        } catch (DataAccessException ex) {
            log.warn("Failed to release game lock {}: {}", key, ex.getMessage());
        }
    }

    private <T> T withLocalLock(String gameId, Supplier<T> action) {
        ReentrantLock lock = localLocks.computeIfAbsent(gameId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
            if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                localLocks.remove(gameId, lock);
            }
        }
    }

    private static void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new GameDomainException.StoreUnavailable();
        }
    }
}
