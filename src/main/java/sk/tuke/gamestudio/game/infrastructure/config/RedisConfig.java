package sk.tuke.gamestudio.game.infrastructure.config;

import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.username:}") String username,
            @Value("${spring.data.redis.password:}") String password) {

        var pool = new GenericObjectPoolConfig<StatefulConnection<?, ?>>();
        pool.setMaxTotal(64);
        pool.setMinIdle(8);
        pool.setMaxIdle(32);
        pool.setTestOnBorrow(false);

        var clientCfg = LettucePoolingClientConfiguration.builder()
                .poolConfig(pool)
                .commandTimeout(Duration.ofMillis(500))
                .build();

        var standaloneCfg = new RedisStandaloneConfiguration(host, port);
        if (!username.isBlank()) {
            standaloneCfg.setUsername(username);
        }
        if (!password.isBlank()) {
            standaloneCfg.setPassword(RedisPassword.of(password));
        }

        return new LettuceConnectionFactory(standaloneCfg, clientCfg);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        var tpl = new RedisTemplate<String, String>();
        tpl.setConnectionFactory(factory);
        tpl.setKeySerializer(RedisSerializer.string());
        tpl.setValueSerializer(RedisSerializer.string());
        tpl.setEnableDefaultSerializer(false);
        tpl.afterPropertiesSet();
        return tpl;
    }
}
