package dev.buildtrace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
    String provider,
    String baseUrl,
    String apiKey,
    String model,
    String codexExecutable,
    String codexModel,
    String codexReasoningEffort,
    Duration timeout
) {
}
