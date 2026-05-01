package sk.tuke.gamestudio.infrastructure.logging;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.core.env.Environment;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupLogger implements ApplicationRunner {

    private final Environment environment;
    private final EntityManagerFactory entityManagerFactory;
    private final ListableBeanFactory beanFactory;
    private final ObjectProvider<CacheManager> cacheManagerProvider;

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String actuatorBasePath;

    @Value("${management.endpoints.web.exposure.include:health,info}")
    private String actuatorExposure;

    @Override
    public void run(ApplicationArguments args) {
        var applicationName = environment.getProperty("spring.application.name", "application");
        var profiles = environment.getActiveProfiles().length == 0
                ? "default"
                : String.join(",", environment.getActiveProfiles());
        var port = environment.getProperty("local.server.port",
                environment.getProperty("server.port", "8080"));
        var datasourceUrl = compactDbUrl(environment.getProperty("spring.datasource.url", "n/a"));
        var redisRepositoriesEnabled = environment.getProperty("spring.data.redis.repositories.enabled", Boolean.class, true);

        var jpaRepositories = beanFactory.getBeanNamesForType(Repository.class).length;
        var loadedEntities = entityManagerFactory.getMetamodel().getEntities().size();
        var cacheWarmupStatus = resolveCacheStatus();

        log.info("\n==================================================\n" +
                        "Startup summary\n" +
                        "--------------------------------------------------\n" +
                        "app={}\nprofiles={}\nport={}\ndbUrl={}\nredisRepositoriesEnabled={}\n" +
                        "actuatorBasePath={}\nactuatorExposure={}\n" +
                        "jpaRepositories={}\nentitiesLoaded={}\ncacheWarmup={}\n" +
                        "==================================================",
                applicationName,
                profiles,
                port,
                datasourceUrl,
                redisRepositoriesEnabled,
                actuatorBasePath,
                normalizeExposure(actuatorExposure),
                jpaRepositories,
                loadedEntities,
                cacheWarmupStatus);
    }

    private String resolveCacheStatus() {
        var cacheManager = cacheManagerProvider.getIfAvailable();
        if (cacheManager == null) {
            return "disabled";
        }
        var cacheNames = cacheManager.getCacheNames();
        if (cacheNames.isEmpty()) {
            return "enabled(no-caches)";
        }
        return "ready(" + String.join(",", cacheNames) + ")";
    }

    private static String compactDbUrl(String url) {
        if (url == null || url.isBlank()) {
            return "n/a";
        }

        var sanitized = url.replaceAll("(jdbc:[^:]+://)([^@/]+)@", "$1***@");
        sanitized = sanitized.replaceAll("\\?.*$", "");
        return sanitized.length() > 120 ? sanitized.substring(0, 117) + "..." : sanitized;
    }

    private static String normalizeExposure(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}
