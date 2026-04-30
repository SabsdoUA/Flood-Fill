package sk.tuke.gamestudio.game.infrastructure.persistence;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import sk.tuke.gamestudio.game.domain.model.GameState;
import sk.tuke.gamestudio.game.domain.port.Ports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RedisGameAdapter implements Ports.GameRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisGameAdapter.class);
    private static final Duration TTL = Duration.ofHours(1);
    private static final String KEY_PREFIX = "game:";

    @Qualifier("redisTemplate")
    private final RedisTemplate<String, String> redis;
    private final Map<String, Snapshot> localSnapshots = new ConcurrentHashMap<>();

    public RedisGameAdapter(@Qualifier("redisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    @Override
    public void save(GameState state) {
        putLocalSnapshot(state);
        try {
            var payload = Base64.getEncoder().encodeToString(GameStateSerializer.serialize(state));
            redis.opsForValue().set(key(state.gameId()), payload, TTL);
        } catch (DataAccessException ex) {
            log.warn("Redis is unavailable while saving game {}. State was not persisted: {}", state.gameId(), ex.getMessage());
        }
    }

    @Override
    public Optional<GameState> findById(String gameId) {
        try {
            var encoded = redis.opsForValue().get(key(gameId));
            if (encoded == null) {
                return findLocalSnapshot(gameId);
            }
            byte[] raw;
            try {
                raw = Base64.getDecoder().decode(encoded);
            } catch (IllegalArgumentException ignored) {
                redis.delete(key(gameId));
                return findLocalSnapshot(gameId);
            }
            redis.expire(key(gameId), TTL); // sliding TTL window
            var state = GameStateSerializer.deserialize(gameId, raw);
            putLocalSnapshot(state);
            return Optional.of(state);
        } catch (DataAccessException ex) {
            log.warn("Redis is unavailable while reading game {}. Returning empty result: {}", gameId, ex.getMessage());
            return findLocalSnapshot(gameId);
        }
    }

    @Override
    public void delete(String gameId) {
        localSnapshots.remove(gameId);
        try {
            redis.delete(key(gameId));
        } catch (DataAccessException ex) {
            log.warn("Redis is unavailable while deleting game {}. Continuing: {}", gameId, ex.getMessage());
        }
    }

    private static String key(String gameId) {
        return KEY_PREFIX + gameId;
    }

    private void putLocalSnapshot(GameState state) {
        localSnapshots.put(state.gameId(), new Snapshot(state, Instant.now().plus(TTL)));
    }

    private Optional<GameState> findLocalSnapshot(String gameId) {
        var snapshot = localSnapshots.get(gameId);
        if (snapshot == null) {
            return Optional.empty();
        }
        if (snapshot.expiresAt().isBefore(Instant.now())) {
            localSnapshots.remove(gameId, snapshot);
            return Optional.empty();
        }
        localSnapshots.put(gameId, new Snapshot(snapshot.state(), Instant.now().plus(TTL)));
        return Optional.of(snapshot.state());
    }

    private record Snapshot(GameState state, Instant expiresAt) {
    }
}
