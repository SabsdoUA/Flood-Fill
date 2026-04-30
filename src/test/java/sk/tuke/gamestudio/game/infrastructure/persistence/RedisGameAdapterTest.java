package sk.tuke.gamestudio.game.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import sk.tuke.gamestudio.game.domain.model.Board;
import sk.tuke.gamestudio.game.domain.model.Color;
import sk.tuke.gamestudio.game.domain.model.GameState;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisGameAdapterTest {

    @Mock private RedisTemplate<String, String> redis;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private RedisGameAdapter adapter;

    private final GameState.Active state =
            new GameState.Active("g1", "qa@example.com", new Board(new Color[][]{{Color.RED}}, 1), 0, 3);

    @Test
    void givenState_whenSave_thenSerializeAndStoreWithTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);

        adapter.save(state);

        verify(valueOps).set(eq("game:g1"), anyString(), eq(Duration.ofHours(1)));
    }

    @Test
    void givenState_whenSave_thenStoreBase64EncodedPayload() {
        when(redis.opsForValue()).thenReturn(valueOps);

        adapter.save(state);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(eq("game:g1"), payload.capture(), eq(Duration.ofHours(1)));
        byte[] decoded = Base64.getDecoder().decode(payload.getValue());
        assertThat(GameStateSerializer.deserialize("g1", decoded)).isEqualTo(state);
    }

    @Test
    void givenMissingEntry_whenFindById_thenReturnEmptyWithoutExpire() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("game:missing")).thenReturn(null);

        Optional<GameState> result = adapter.findById("missing");

        assertThat(result).isEmpty();
        verify(redis, never()).expire(anyString(), any());
    }

    @Test
    void givenExistingEntry_whenFindById_thenDeserializeAndRefreshTtl() {
        when(redis.opsForValue()).thenReturn(valueOps);
        byte[] raw = GameStateSerializer.serialize(state);
        when(valueOps.get("game:g1")).thenReturn(Base64.getEncoder().encodeToString(raw));

        Optional<GameState> result = adapter.findById("g1");

        assertThat(result).contains(state);
        verify(redis).expire("game:g1", Duration.ofHours(1));
    }

    @Test
    void givenCorruptedPayload_whenFindById_thenDeleteAndReturnEmpty() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("game:g1")).thenReturn("not-base64%%%");

        Optional<GameState> result = adapter.findById("g1");

        assertThat(result).isEmpty();
        verify(redis).delete("game:g1");
        verify(redis, never()).expire(anyString(), any());
    }

    @Test
    void givenGameId_whenDelete_thenDeleteByPrefixedKey() {
        adapter.delete("g1");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(redis).delete(key.capture());
        assertThat(key.getValue()).isEqualTo("game:g1");
    }

    @Test
    void givenRedisUnavailable_whenSave_thenDoNotThrow() {
        when(redis.opsForValue()).thenReturn(valueOps);
        doThrow(new RedisConnectionFailureException("down"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        adapter.save(state);

        verify(valueOps).set(eq("game:g1"), anyString(), eq(Duration.ofHours(1)));
    }

    @Test
    void givenRedisUnavailableAfterSave_whenFindById_thenReturnEmpty() {
        when(redis.opsForValue()).thenReturn(valueOps);
        doThrow(new RedisConnectionFailureException("down"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));
        when(valueOps.get("game:g1")).thenThrow(new RedisConnectionFailureException("down"));

        adapter.save(state);
        Optional<GameState> result = adapter.findById("g1");

        assertThat(result).contains(state);
    }

    @Test
    void givenRedisUnavailableWithoutLocalSnapshot_whenFindById_thenReturnEmpty() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("game:g1")).thenThrow(new RedisConnectionFailureException("down"));

        Optional<GameState> result = adapter.findById("g1");

        assertThat(result).isEmpty();
    }

    @Test
    void givenRedisUnavailable_whenDelete_thenDoNotThrow() {
        doThrow(new RedisConnectionFailureException("down")).when(redis).delete("game:g1");

        adapter.delete("g1");

        verify(redis).delete("game:g1");
    }

    @Test
    void givenRedisReturnsNull_whenLocalSnapshotExists_thenReturnLocalSnapshot() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("game:g1")).thenReturn(null);

        adapter.save(state);
        Optional<GameState> result = adapter.findById("g1");

        assertThat(result).contains(state);
    }

    @Test
    void givenCorruptedRedisPayload_whenLocalSnapshotExists_thenReturnLocalSnapshot() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("game:g1")).thenReturn("%%%");

        adapter.save(state);
        Optional<GameState> result = adapter.findById("g1");

        assertThat(result).contains(state);
        verify(redis).delete("game:g1");
    }
}
