package dev.buildtrace.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StructuredOutputParser {

    private final ObjectMapper objectMapper;

    public StructuredOutputParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GenerationResult parse(String output) {
        if (output == null || output.isBlank()) {
            throw new IllegalArgumentException("Model returned an empty response");
        }
        int start = output.indexOf('{');
        int end = output.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Model response does not contain a JSON object");
        }
        try {
            GenerationResult result = objectMapper.readValue(output.substring(start, end + 1), GenerationResult.class);
            if (result.operations() == null || result.operations().isEmpty()) {
                throw new IllegalArgumentException("Model returned no file operations");
            }
            if (result.operations().size() > 30) {
                throw new IllegalArgumentException("Model returned too many file operations");
            }
            if (result.plan() != null && result.plan().size() > 12) {
                throw new IllegalArgumentException("Model returned too many plan steps");
            }
            if (result.checks() != null && result.checks().size() > 12) {
                throw new IllegalArgumentException("Model returned too many verification checks");
            }
            return new GenerationResult(
                textOr(result.understanding(), "理解并实现本轮需求"),
                cleanList(result.plan()),
                textOr(result.summary(), "已完成本轮实现"),
                result.operations(),
                cleanList(result.checks())
            );
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Model returned invalid structured JSON", exception);
        }
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .toList();
    }

    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
