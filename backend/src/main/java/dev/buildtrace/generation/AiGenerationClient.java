package dev.buildtrace.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.buildtrace.config.AiProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
public class AiGenerationClient {

    private static final String SYSTEM_PROMPT = """
        You are the implementation agent inside an AI app builder.
        Return exactly one complete, self-contained HTML document with inline CSS and JavaScript.
        Do not use Markdown fences, explanations, external packages, remote scripts, or external images.
        The result must be polished, responsive, accessible, and genuinely interactive.
        Every requested control must work in the browser. Prefer deterministic local sample data.
        The app runs in a sandbox without same-origin access. Never use localStorage, sessionStorage,
        IndexedDB, cookies, service workers, or any API that requires a normal document origin.
        Keep demo state in JavaScript memory and prevent default form navigation.
        """;

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final CodexCliGenerationClient codexCliClient;

    public AiGenerationClient(
        AiProperties properties,
        ObjectMapper objectMapper,
        WebClient.Builder builder,
        CodexCliGenerationClient codexCliClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = builder.baseUrl(properties.baseUrl()).build();
        this.codexCliClient = codexCliClient;
    }

    public boolean configured() {
        if (usesCodexCli()) {
            return true;
        }
        return properties.apiKey() != null && !properties.apiKey().isBlank();
    }

    public String model() {
        if (usesCodexCli()) {
            return properties.codexModel() == null || properties.codexModel().isBlank()
                ? "Codex CLI"
                : "Codex CLI / " + properties.codexModel();
        }
        return properties.model();
    }

    public Flux<String> stream(String prompt, String currentHtml) {
        String userPrompt = buildUserPrompt(prompt, currentHtml);
        if (usesCodexCli()) {
            return codexCliClient.stream(SYSTEM_PROMPT + "\n\n" + userPrompt);
        }

        Map<String, Object> request = Map.of(
            "model", properties.model(),
            "stream", true,
            "temperature", 0.2,
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
            )
        );

        return webClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .headers(headers -> headers.setBearerAuth(properties.apiKey()))
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() { })
            .mapNotNull(ServerSentEvent::data)
            .takeUntil("[DONE]"::equals)
            .filter(data -> !"[DONE]".equals(data))
            .mapNotNull(this::contentFromChunk)
            .timeout(properties.timeout());
    }

    private String buildUserPrompt(String prompt, String currentHtml) {
        return currentHtml == null || currentHtml.isBlank()
            ? "Create this application:\n" + prompt
            : "Update the current application according to the request. Return the entire updated HTML."
                + "\n\nRequest:\n" + prompt + "\n\nCurrent HTML:\n" + currentHtml;
    }

    private boolean usesCodexCli() {
        return "codex-cli".equalsIgnoreCase(properties.provider());
    }

    private String contentFromChunk(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode content = root.path("choices").path(0).path("delta").path("content");
            return content.isTextual() ? content.asText() : null;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Model returned an invalid streaming chunk", exception);
        }
    }
}
