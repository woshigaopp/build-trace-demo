package dev.buildtrace.generation;

import java.util.List;

public record GenerationResult(
    String understanding,
    List<String> plan,
    String summary,
    List<FileOperation> operations,
    List<String> checks
) {

    public GenerationResult(String summary, List<FileOperation> operations) {
        this(summary, List.of(), summary, operations, List.of());
    }

    public record FileOperation(String type, String path, String content) {
    }
}
