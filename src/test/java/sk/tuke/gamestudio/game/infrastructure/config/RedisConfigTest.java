package sk.tuke.gamestudio.game.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    private final RedisConfig config = new RedisConfig();

    @Test
    void givenHostAndPort_whenCreateConnectionFactory_thenReturnLettuceFactory() {
        RedisConnectionFactory factory = config.redisConnectionFactory("localhost", 6379, "default", "root");

        assertThat(factory).isInstanceOf(LettuceConnectionFactory.class);
    }

    @Test
    void givenConnectionFactory_whenCreateRedisTemplate_thenSetupSerializersAndFactory() {
        RedisConnectionFactory factory = config.redisConnectionFactory("localhost", 6379, "", "");

        RedisTemplate<String, String> template = config.redisTemplate(factory);

        assertThat(template.getConnectionFactory()).isSameAs(factory);
        assertThat(template.isEnableDefaultSerializer()).isFalse();
        assertThat(template.getKeySerializer()).isNotNull();
        assertThat(template.getValueSerializer()).isNotNull();
    }
}
