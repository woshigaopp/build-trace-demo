package dev.buildtrace.generation;

import java.util.List;

public record GenerationResult(String summary, List<FileOperation> operations) {

    public record FileOperation(String type, String path, String content) {
    }
}
