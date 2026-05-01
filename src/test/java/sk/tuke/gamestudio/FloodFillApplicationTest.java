package sk.tuke.gamestudio;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FloodFillApplicationTest {

    @Test
    void whenMainIsCalled_thenSpringApplicationRunIsInvoked() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            FloodFillApplication.main(new String[]{"--spring.main.web-application-type=none"});

            springApplication.verify(() -> SpringApplication.run(eq(FloodFillApplication.class), eq(new String[]{"--spring.main.web-application-type=none"})));
        }
    }

    @Test
    void givenNoNettyProperty_whenClassLoaded_thenDefaultNoUnsafeTrue() {
        assertThat(System.getProperty("io.netty.noUnsafe")).isEqualTo("true");
    }

    @Test
    void givenActiveProfiles_whenStartupLoggerRuns_thenListenerExecutes() {
        FloodFillApplication app = new FloodFillApplication();
        Environment environment = mock(Environment.class);

        when(environment.getProperty("server.port", "8080")).thenReturn("9090");
        when(environment.getProperty("PORT", "8080")).thenReturn("9090");
        when(environment.getProperty("server.address", "0.0.0.0")).thenReturn("127.0.0.1");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev", "local"});

        ApplicationListener<ApplicationStartedEvent> listener = app.startupConfigLogger(environment);

        listener.onApplicationEvent(null);

        verify(environment).getProperty("PORT", "8080");
        verify(environment).getProperty("server.port", "9090");
        verify(environment).getProperty("server.address", "0.0.0.0");
        verify(environment).getActiveProfiles();
    }

    @Test
    void givenBlankProfilesAndMissingServerPort_whenStartupLoggerRuns_thenFallbackToPortEnvAndDefaultProfile() {
        FloodFillApplication app = new FloodFillApplication();
        Environment environment = mock(Environment.class);

        when(environment.getProperty("PORT", "8080")).thenReturn("5050");
        when(environment.getProperty("server.port", "5050")).thenReturn("5050");
        when(environment.getProperty("server.address", "0.0.0.0")).thenReturn("0.0.0.0");
        when(environment.getActiveProfiles()).thenReturn(new String[]{});

        ApplicationListener<ApplicationStartedEvent> listener = app.startupConfigLogger(environment);

        listener.onApplicationEvent(null);

        verify(environment).getProperty("PORT", "8080");
        verify(environment).getProperty("server.port", "5050");
    }

    @Test
    void givenFailureEvent_whenStartupFailureLoggerRuns_thenListenerExecutes() {
        FloodFillApplication app = new FloodFillApplication();
        ApplicationListener<ApplicationFailedEvent> listener = app.startupFailureLogger();

        listener.onApplicationEvent(new ApplicationFailedEvent(new SpringApplication(FloodFillApplication.class), new String[]{}, null, new RuntimeException("boom")));
    }

    @Test
    void givenRedisPingSucceeds_whenCheckRedisRuns_thenPingIsCalled() throws Exception {
        FloodFillApplication app = new FloodFillApplication();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        Environment environment = mock(Environment.class);
        when(environment.getProperty("app.redis.fail-fast", Boolean.class, false)).thenReturn(false);

        CommandLineRunner runner = app.checkRedis(redisTemplate, environment);
        runner.run();

        verify(connection).ping();
    }

    @Test
    void givenRedisPingFails_whenCheckRedisRuns_thenExceptionIsSwallowed() throws Exception {
        FloodFillApplication app = new FloodFillApplication();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenThrow(new RuntimeException("redis down"));

        Environment environment = mock(Environment.class);
        when(environment.getProperty("app.redis.fail-fast", Boolean.class, false)).thenReturn(false);

        CommandLineRunner runner = app.checkRedis(redisTemplate, environment);
        runner.run();

        verify(connection).ping();
    }

    @Test
    void givenRedisPingFailsAndFailFastEnabled_whenCheckRedisRuns_thenThrow() throws Exception {
        FloodFillApplication app = new FloodFillApplication();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        Environment environment = mock(Environment.class);

        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenThrow(new RuntimeException("redis down"));
        when(environment.getProperty("app.redis.fail-fast", Boolean.class, false)).thenReturn(true);

        CommandLineRunner runner = app.checkRedis(redisTemplate, environment);

        org.assertj.core.api.Assertions.assertThatThrownBy(runner::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis ping failed");
    }
}
