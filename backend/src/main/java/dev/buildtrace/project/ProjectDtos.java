package dev.buildtrace.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
        @NotBlank @Size(max = 160) String name
    ) {
    }

    public record GenerateRequest(
        @NotBlank @Size(max = 4_000) String prompt
    ) {
    }

    public record ProjectSummary(
        String id,
        String name,
        boolean hasPreview,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record MessageResponse(
        String id,
        String role,
        String content,
        Instant createdAt
    ) {
    }

    public record VersionResponse(
        String id,
        int versionNumber,
        String prompt,
        Instant createdAt
    ) {
    }

    public record ProjectDetail(
        String id,
        String name,
        String currentHtml,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages,
        List<VersionResponse> versions
    ) {
    }
}
