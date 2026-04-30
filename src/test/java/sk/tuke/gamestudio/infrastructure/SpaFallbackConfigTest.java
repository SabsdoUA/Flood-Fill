package sk.tuke.gamestudio.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;
import sk.tuke.gamestudio.infrastructure.web.SpaFallbackConfig;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SpaFallbackConfigTest {

    private final Path classpathRoot = resolveClasspathRoot();
    private final Path staticDir = classpathRoot.resolve("static");
    private final ResourceResolverChain emptyChain = mock(ResourceResolverChain.class);

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(staticDir.resolve("index.html"));
        Files.deleteIfExists(staticDir.resolve("existing.txt"));
        if (Files.exists(staticDir) && isDirectoryEmpty(staticDir)) {
            Files.deleteIfExists(staticDir);
        }
    }

    @Test
    void givenExistingResource_whenResolve_thenReturnRequestedResource() throws Exception {
        Files.createDirectories(staticDir);
        Files.writeString(staticDir.resolve("existing.txt"), "ok");

        Resource resolved = resolveWithConfig("existing.txt");

        assertThat(resolved).isNotNull();
        assertThat(resolved.exists()).isTrue();
        assertThat(resolved.getFilename()).isEqualTo("existing.txt");
    }

    @Test
    void givenMissingResourceAndMissingIndex_whenResolve_thenReturnNull() {
        Resource resolved = resolveWithConfig("missing-route");

        assertThat(resolved).isNull();
    }

    @Test
    void givenMissingResourceAndExistingIndex_whenResolve_thenReturnIndexHtml() throws Exception {
        Files.createDirectories(staticDir);
        Files.writeString(staticDir.resolve("index.html"), "<html>spa</html>");

        Resource resolved = resolveWithConfig("client/route");

        assertThat(resolved).isNotNull();
        assertThat(resolved.exists()).isTrue();
        assertThat(resolved.getFilename()).isEqualTo("index.html");
    }

    @Test
    void givenBackendPrefixedRouteAndExistingIndex_whenResolve_thenReturnNull() throws Exception {
        Files.createDirectories(staticDir);
        Files.writeString(staticDir.resolve("index.html"), "<html>spa</html>");

        Resource resolved = resolveWithConfig("actuator/unknown");

        assertThat(resolved).isNull();
    }

    @Test
    void givenMissingFileWithExtensionAndExistingIndex_whenResolve_thenReturnNull() throws Exception {
        Files.createDirectories(staticDir);
        Files.writeString(staticDir.resolve("index.html"), "<html>spa</html>");

        Resource resolved = resolveWithConfig("scripts/app.js");

        assertThat(resolved).isNull();
    }

    private Resource resolveWithConfig(String path) {
        ResourceResolver resolver = resolverFromConfig();
        try {
            List<Resource> locations = List.of(new UrlResource(staticDir.toUri()));
            return resolver.resolveResource(mock(HttpServletRequest.class), path, locations, emptyChain);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot build resource location", e);
        }
    }

    @SuppressWarnings("unchecked")
    private ResourceResolver resolverFromConfig() {
        SpaFallbackConfig config = new SpaFallbackConfig();
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(new StaticApplicationContext(), new MockServletContext());
        config.addResourceHandlers(registry);

        List<ResourceHandlerRegistration> registrations =
                (List<ResourceHandlerRegistration>) ReflectionTestUtils.getField(registry, "registrations");
        assertThat(registrations).hasSize(1);

        Object chain = ReflectionTestUtils.getField(registrations.getFirst(), "resourceChainRegistration");
        assertThat(chain).isNotNull();

        List<ResourceResolver> resolvers = (List<ResourceResolver>) ReflectionTestUtils.getField(chain, "resolvers");
        assertThat(resolvers).hasSizeGreaterThanOrEqualTo(1);
        return resolvers.getLast();
    }

    private Path resolveClasspathRoot() {
        try {
            return Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("")).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot resolve classpath root", e);
        }
    }

    private boolean isDirectoryEmpty(Path dir) throws Exception {
        try (var stream = Files.list(dir)) {
            return stream.findAny().isEmpty();
        }
    }
}
