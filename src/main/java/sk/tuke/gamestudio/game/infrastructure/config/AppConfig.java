package sk.tuke.gamestudio.game.infrastructure.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import sk.tuke.gamestudio.game.domain.model.Color;

import java.io.IOException;

@Configuration
public class AppConfig {

    @Bean("asyncExecutor")
    public AsyncTaskExecutor asyncExecutor() {
        var executor = new SimpleAsyncTaskExecutor("async-vt-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean
    public com.fasterxml.jackson.databind.Module colorModule() {
        var module = new SimpleModule();
        module.addSerializer(Color[][].class, new ColorSerializer());
        return module;
    }

    private static final class ColorSerializer extends JsonSerializer<Color[][]> {
        @Override
        public void serialize(Color[][] grid, JsonGenerator gen, SerializerProvider p)
                throws IOException {
            gen.writeStartArray();
            for (var row : grid) {
                gen.writeStartArray();
                for (var cell : row) gen.writeString(cell.name());
                gen.writeEndArray();
            }
            gen.writeEndArray();
        }
    }
}