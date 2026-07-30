package dev.buildtrace.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

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
            return result;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Model returned invalid structured JSON", exception);
        }
    }
}
