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
        You are the implementation agent inside BuildTrace, a multi-file React app builder.
        Respond with exactly one JSON object and no Markdown or explanation:
        {"understanding":"one precise Chinese sentence","plan":["concrete implementation step"],"summary":"short user-facing Chinese delivery summary","operations":[{"type":"write","path":"/App.jsx","content":"complete file content"}],"checks":["user-visible behavior to verify"]}

        Rules:
        - Use only operation types "write" and "delete". Paths must be absolute project paths.
        - Every write contains the COMPLETE new content of that file, never a patch or ellipsis.
        - Prefer changing only files needed for this request. Do not rewrite /package.json, /index.html or /index.jsx unless required.
        - The app must remain a runnable Vite React app using the root-level scaffold. /App.jsx exports a default component and /styles.css contains its styles.
        - Put additional React components under /components. Never create a second /src application tree.
        - Use React and browser APIs only. Do not add external dependencies, remote scripts, external images, network calls or secrets.
        - Every requested control must really work. Use React state and realistic local sample data; never render dead buttons.
        - When the app owns editable domain data, persist it with localStorage and handle an empty collection gracefully.
        - Deliver a complete product surface: clear information hierarchy, useful initial data, empty/error states, responsive mobile layout and accessible labels.
        - Use restrained product styling with design tokens, consistent spacing, visible focus states and at most 8px card radii. Avoid gradients, giant marketing headings and decorative blobs.
        - The plan must name the concrete product behavior and file responsibilities. The checks are behaviors the user should inspect, not claims that a compiler already ran.
        - Keep the result responsive, accessible and visually polished. Do not return placeholder prose about future work.
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
        return usesCodexCli() || (properties.apiKey() != null && !properties.apiKey().isBlank());
    }

    public String model() {
        if (usesCodexCli()) {
            return properties.codexModel() == null || properties.codexModel().isBlank()
                ? "Codex CLI" : "Codex CLI / " + properties.codexModel();
        }
        return properties.model();
    }

    public Flux<String> stream(String prompt, Map<String, String> currentFiles) {
        String intent = currentFiles.isEmpty()
            ? "Create the requested application. The server will provide the standard React scaffold."
            : "Modify the current application while preserving behavior that the request does not change.";
        return streamPrompt(intent + "\n\nUser request:\n" + prompt + "\n\nCurrent files JSON:\n" + json(currentFiles));
    }

    public Flux<String> repair(
        String prompt,
        Map<String, String> currentFiles,
        String invalidOutput,
        String validationError
    ) {
        String clippedOutput = invalidOutput.length() > 24_000 ? invalidOutput.substring(0, 24_000) : invalidOutput;
        return streamPrompt("""
            Repair the previous response. Return a corrected JSON object with understanding, plan, summary, operations and checks that satisfies the exact schema and preserves the user intent.

            User request:
            %s

            Current files JSON:
            %s

            Validation error:
            %s

            Invalid response:
            %s
            """.formatted(prompt, json(currentFiles), validationError, clippedOutput));
    }

    private Flux<String> streamPrompt(String userPrompt) {
        if (usesCodexCli()) {
            return codexCliClient.stream(SYSTEM_PROMPT + "\n\n" + userPrompt);
        }
        Map<String, Object> request = Map.of(
            "model", properties.model(),
            "stream", true,
            "temperature", 0.1,
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

    private String json(Map<String, String> files) {
        try {
            return objectMapper.writeValueAsString(files);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to serialize project context", exception);
        }
    }
}
