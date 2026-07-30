package dev.buildtrace;

import dev.buildtrace.config.AiProperties;
import dev.buildtrace.auth.AuthProperties;
import dev.buildtrace.config.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AiProperties.class, AuthProperties.class, CorsProperties.class})
public class BuildTraceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuildTraceApplication.class, args);
    }
}
