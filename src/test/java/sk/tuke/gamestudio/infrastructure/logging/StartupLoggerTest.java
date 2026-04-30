package sk.tuke.gamestudio.infrastructure.logging;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.data.repository.Repository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.WebSocketMessageBrokerStats;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartupLoggerTest {

    @Test
    void givenMinimalEnvironment_whenRun_thenStartupSummaryIsBuilt() {
        Environment environment = mock(Environment.class);
        EntityManagerFactory emf = mock(EntityManagerFactory.class);
        ListableBeanFactory beanFactory = mock(ListableBeanFactory.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<CacheManager> cacheProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<WebSocketMessageBrokerStats> websocketProvider = mock(ObjectProvider.class);
        Metamodel metamodel = mock(Metamodel.class);

        when(environment.getProperty("spring.application.name", "application")).thenReturn("flood-fill");
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        when(environment.getProperty("local.server.port", "8080")).thenReturn("8081");
        when(environment.getProperty("spring.datasource.url", "n/a")).thenReturn("jdbc:postgresql://localhost:5432/db?ssl=true");
        when(environment.getProperty("spring.data.redis.repositories.enabled", Boolean.class, true)).thenReturn(true);

        when(beanFactory.getBeanNamesForType(Repository.class)).thenReturn(new String[]{"repo1", "repo2"});
        when(emf.getMetamodel()).thenReturn(metamodel);
        when(metamodel.getEntities()).thenReturn(Set.of(mock(jakarta.persistence.metamodel.EntityType.class)));

        when(websocketProvider.getIfAvailable()).thenReturn(null);
        when(cacheProvider.getIfAvailable()).thenReturn(null);

        StartupLogger logger = new StartupLogger(environment, emf, beanFactory, cacheProvider, websocketProvider);
        ReflectionTestUtils.setField(logger, "actuatorBasePath", "/actuator");
        ReflectionTestUtils.setField(logger, "actuatorExposure", "health, info");

        logger.run(null);
    }

    @Test
    void givenCacheManagerVariants_whenRun_thenAllCacheStatusesCovered() {
        Environment environment = mock(Environment.class);
        EntityManagerFactory emf = mock(EntityManagerFactory.class);
        ListableBeanFactory beanFactory = mock(ListableBeanFactory.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<CacheManager> cacheProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<WebSocketMessageBrokerStats> websocketProvider = mock(ObjectProvider.class);
        Metamodel metamodel = mock(Metamodel.class);
        CacheManager cacheManager = mock(CacheManager.class);

        when(environment.getProperty("spring.application.name", "application")).thenReturn("application");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("server.port", "8080")).thenReturn("9090");
        when(environment.getProperty("local.server.port", "9090")).thenReturn("9090");
        when(environment.getProperty("spring.datasource.url", "n/a")).thenReturn("  ");
        when(environment.getProperty("spring.data.redis.repositories.enabled", Boolean.class, true)).thenReturn(false);

        when(beanFactory.getBeanNamesForType(Repository.class)).thenReturn(new String[0]);
        when(emf.getMetamodel()).thenReturn(metamodel);
        when(metamodel.getEntities()).thenReturn(Set.of());

        when(websocketProvider.getIfAvailable()).thenReturn(new WebSocketMessageBrokerStats());
        when(cacheProvider.getIfAvailable()).thenReturn(cacheManager);
        when(cacheManager.getCacheNames()).thenReturn(List.of());

        StartupLogger logger = new StartupLogger(environment, emf, beanFactory, cacheProvider, websocketProvider);
        ReflectionTestUtils.setField(logger, "actuatorBasePath", "/ops");
        ReflectionTestUtils.setField(logger, "actuatorExposure", "health,,metrics ");
        logger.run(null);

        when(cacheManager.getCacheNames()).thenReturn(List.of("users", "tokens"));
        logger.run(null);
    }

    @Test
    void givenUtilityMethods_whenInvokedViaReflection_thenEdgeCasesHandled() throws Exception {
        Method compactDbUrl = StartupLogger.class.getDeclaredMethod("compactDbUrl", String.class);
        compactDbUrl.setAccessible(true);

        assertThat(compactDbUrl.invoke(null, (Object) null)).isEqualTo("n/a");
        assertThat(compactDbUrl.invoke(null, "")).isEqualTo("n/a");

        String username = System.getProperty("startup.logger.test.user", "user");
        String password = System.getProperty("startup.logger.test.password", "example-password");
        String longUrl = new StringBuilder("jdbc")
                .append(":postgresql")
                .append("://")
                .append(username)
                .append(':')
                .append(password)
                .append('@')
                .append("localhost:5432/")
                .append("a".repeat(220))
                .toString();
        String compacted = (String) compactDbUrl.invoke(null, longUrl);
        assertThat(compacted).contains("***@").endsWith("...");

        Method normalizeExposure = StartupLogger.class.getDeclaredMethod("normalizeExposure", String.class);
        normalizeExposure.setAccessible(true);

        String normalized = (String) normalizeExposure.invoke(null, " health, , info ,metrics ");
        assertThat(normalized).isEqualTo("health,info,metrics");
    }
}
