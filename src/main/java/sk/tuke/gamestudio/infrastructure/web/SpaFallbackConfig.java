package sk.tuke.gamestudio.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

@Configuration
public class SpaFallbackConfig implements WebMvcConfigurer {

    private static final List<String> API_PREFIXES = List.of(
            "api/",
            "auth/",
            "secured/",
            "actuator/",
            "oauth2/",
            "login/",
            "logout"
    );

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (!shouldServeSpaFallback(resourcePath)) {
                            return null;
                        }
                        // SPA fallback: serve index.html for all client-side routes
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }

    private static boolean shouldServeSpaFallback(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return true;
        }

        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        String lower = normalized.toLowerCase();

        if (lower.contains(".")) {
            return false;
        }

        return API_PREFIXES.stream().noneMatch(lower::startsWith);
    }
}
