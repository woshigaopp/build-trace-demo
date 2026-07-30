package dev.buildtrace.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
        String runId,
        String status,
        Instant createdAt
    ) {
    }

    public record VersionResponse(
        String id,
        int versionNumber,
        String prompt,
        String source,
        String summary,
        int fileCount,
        Instant createdAt
    ) {
    }

    public record GenerationRunResponse(
        String id,
        String prompt,
        String status,
        String model,
        int attemptCount,
        String errorMessage,
        Long durationMs,
        String understanding,
        List<String> plan,
        List<String> changedFiles,
        List<String> checks,
        List<TraceEventResponse> trace,
        Integer deliveredVersionNumber,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record TraceEventResponse(
        String status,
        String title,
        String detail,
        int attempt,
        Instant createdAt
    ) {
    }

    public record PublicationResponse(
        String token,
        String versionId,
        int versionNumber,
        Instant publishedAt
    ) {
    }

    public record PublishedProjectResponse(
        String name,
        int versionNumber,
        Map<String, String> files,
        Instant publishedAt
    ) {
    }

    public record SaveVersionRequest(
        Map<String, String> files,
        @Size(max = 500) String summary
    ) {
    }

    public record VersionDetail(
        String id,
        int versionNumber,
        String prompt,
        String source,
        String summary,
        Map<String, String> files,
        Instant createdAt
    ) {
    }

    public record ProjectDetail(
        String id,
        String name,
        String currentVersionId,
        Map<String, String> currentFiles,
        PublicationResponse publication,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages,
        List<VersionResponse> versions,
        List<GenerationRunResponse> runs
    ) {
    }

    public record GenerationContext(
        String runId,
        String projectId,
        String prompt,
        Map<String, String> currentFiles
    ) {
    }
}
