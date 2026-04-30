package sk.tuke.gamestudio.game.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import sk.tuke.gamestudio.game.domain.model.Color;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    private final AppConfig config = new AppConfig();

    @Test
    void givenAsyncExecutorBean_whenCreated_thenUsesConfiguredPrefix() {
        var executor = config.asyncExecutor();

        assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);
        assertThat(((SimpleAsyncTaskExecutor) executor).getThreadNamePrefix()).isEqualTo("async-vt-");
    }

    @Test
    void givenColorGrid_whenSerializeWithModule_thenColorNamesAreWritten() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(config.colorModule());

        String json = mapper.writeValueAsString(new Color[][]{{Color.RED, Color.BLUE}});

        assertThat(json).isEqualTo("[[\"RED\",\"BLUE\"]]");
    }
}
