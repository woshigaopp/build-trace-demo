package dev.buildtrace.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructuredGenerationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StructuredOutputParser parser = new StructuredOutputParser(objectMapper);
    private final ProjectFiles projectFiles = new ProjectFiles(objectMapper);

    @Test
    void parsesFencedJsonAndAppliesOnlyRequestedFiles() {
        GenerationResult result = parser.parse("""
            Result:
            ```json
            {"summary":"updated","operations":[{"type":"write","path":"App.jsx","content":"import React from 'react'; export default function App(){ return <h1>Updated</h1>; }"}]}
            ```
            """);

        Map<String, String> candidate = projectFiles.candidate(Map.of(), result);

        assertThat(candidate).containsKeys("/package.json", "/index.jsx", "/App.jsx", "/styles.css");
        assertThat(candidate.get("/App.jsx")).contains("Updated");
    }

    @Test
    void rejectsTraversalAndDeletionOfRequiredScaffold() {
        GenerationResult traversal = new GenerationResult("bad", java.util.List.of(
            new GenerationResult.FileOperation("write", "../secret", "value")));
        GenerationResult deleteApp = new GenerationResult("bad", java.util.List.of(
            new GenerationResult.FileOperation("delete", "/App.jsx", null)));

        assertThatThrownBy(() -> projectFiles.candidate(Map.of(), traversal))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unsafe file path");
        assertThatThrownBy(() -> projectFiles.candidate(Map.of(), deleteApp))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("required React scaffold");
    }

    @Test
    void rejectsMalformedOrEmptyStructuredResponses() {
        assertThatThrownBy(() -> parser.parse("not json"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("JSON object");
        assertThatThrownBy(() -> parser.parse("{\"summary\":\"none\",\"operations\":[]}"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no file operations");
    }
}
