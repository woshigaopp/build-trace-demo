package dev.buildtrace.project;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.buildtrace.generation.ProjectFiles;
import dev.buildtrace.project.ProjectDtos.GenerationContext;
import dev.buildtrace.project.ProjectDtos.GenerationRunResponse;
import dev.buildtrace.project.ProjectDtos.MessageResponse;
import dev.buildtrace.project.ProjectDtos.ProjectDetail;
import dev.buildtrace.project.ProjectDtos.ProjectSummary;
import dev.buildtrace.project.ProjectDtos.VersionDetail;
import dev.buildtrace.project.ProjectDtos.VersionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class ProjectService {

    private static final List<String> ACTIVE_RUN_STATUSES = EnumSet.of(
        GenerationRunStatus.QUEUED,
        GenerationRunStatus.GENERATING,
        GenerationRunStatus.VALIDATING,
        GenerationRunStatus.REPAIRING
    ).stream().map(Enum::name).toList();

    private final ProjectRepository projectRepository;
    private final MessageRepository messageRepository;
    private final VersionRepository versionRepository;
    private final GenerationRunRepository runRepository;
    private final ObjectMapper objectMapper;
    private final ProjectFiles projectFiles;
    private final ExecutorService executor;

    public ProjectService(
        ProjectRepository projectRepository,
        MessageRepository messageRepository,
        VersionRepository versionRepository,
        GenerationRunRepository runRepository,
        ObjectMapper objectMapper,
        ProjectFiles projectFiles,
        ExecutorService executor
    ) {
        this.projectRepository = projectRepository;
        this.messageRepository = messageRepository;
        this.versionRepository = versionRepository;
        this.runRepository = runRepository;
        this.objectMapper = objectMapper;
        this.projectFiles = projectFiles;
        this.executor = executor;
    }

    @Transactional
    public ProjectDetail create(String ownerId, String name) {
        Instant now = Instant.now();
        ProjectEntity project = new ProjectEntity(UUID.randomUUID().toString(), ownerId, name.trim(), now);
        projectRepository.save(project);
        return toDetail(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> list(String ownerId) {
        return projectRepository.findAllByGuestIdOrderByUpdatedAtDesc(ownerId).stream()
            .map(this::toSummary)
            .toList();
    }

    public ProjectDetail get(String ownerId, String projectId) {
        ProjectEntity project = requireProject(ownerId, projectId);
        CompletableFuture<List<MessageEntity>> messages = CompletableFuture.supplyAsync(
            () -> messageRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId), executor);
        CompletableFuture<List<VersionEntity>> versions = CompletableFuture.supplyAsync(
            () -> versionRepository.findAllByProjectIdOrderByVersionNumberDesc(projectId), executor);
        CompletableFuture<List<GenerationRunEntity>> runs = CompletableFuture.supplyAsync(
            () -> runRepository.findAllByProjectIdOrderByCreatedAtAsc(projectId), executor);
        CompletableFuture.allOf(messages, versions, runs).join();
        return toDetail(project, messages.join(), versions.join(), runs.join());
    }

    @Transactional
    public GenerationContext beginGeneration(String ownerId, String projectId, String prompt, String model) {
        ProjectEntity project = requireProject(ownerId, projectId);
        runRepository.findFirstByProjectIdAndStatusIn(projectId, ACTIVE_RUN_STATUSES).ifPresent(run -> {
            throw new IllegalStateException("当前项目已有生成任务，请等待完成后再试");
        });

        Instant now = Instant.now();
        String runId = UUID.randomUUID().toString();
        runRepository.save(new GenerationRunEntity(runId, projectId, prompt, model, now));
        messageRepository.save(new MessageEntity(
            UUID.randomUUID().toString(), projectId, "user", prompt, runId, "accepted", now));
        return new GenerationContext(runId, projectId, prompt, currentFiles(project));
    }

    @Transactional
    public void transitionRun(String ownerId, String projectId, String runId, GenerationRunStatus status, int attempt) {
        requireProject(ownerId, projectId);
        GenerationRunEntity run = requireRun(projectId, runId);
        if (GenerationRunStatus.valueOf(run.getStatus()).terminal()) {
            throw new IllegalStateException("Generation run is already terminal");
        }
        run.transition(status, attempt);
        runRepository.save(run);
    }

    @Transactional
    public ProjectDetail completeGeneration(
        String ownerId,
        String projectId,
        String runId,
        String prompt,
        Map<String, String> files,
        String summary,
        long durationMs
    ) {
        ProjectEntity project = requireProject(ownerId, projectId);
        GenerationRunEntity run = requireRun(projectId, runId);
        projectFiles.validate(files);
        VersionEntity version = createVersion(project, files, prompt, "ai", runId, summary);
        run.succeed(durationMs);
        runRepository.save(run);
        messageRepository.save(new MessageEntity(
            UUID.randomUUID().toString(),
            projectId,
            "assistant",
            summary == null || summary.isBlank()
                ? "已生成版本 v" + version.getVersionNumber() + "，文件已通过校验。"
                : summary,
            runId,
            "succeeded",
            Instant.now()
        ));
        return toDetail(project);
    }

    @Transactional
    public ProjectDetail failGeneration(
        String ownerId,
        String projectId,
        String runId,
        String error,
        long durationMs
    ) {
        ProjectEntity project = requireProject(ownerId, projectId);
        GenerationRunEntity run = requireRun(projectId, runId);
        if (!GenerationRunStatus.valueOf(run.getStatus()).terminal()) {
            String safeError = conciseError(error);
            run.fail(safeError, durationMs);
            runRepository.save(run);
            messageRepository.save(new MessageEntity(
                UUID.randomUUID().toString(), projectId, "assistant",
                "本次生成未能完成：" + safeError + "。当前版本未被修改，可以直接重试。",
                runId, "failed", Instant.now()));
        }
        return toDetail(project);
    }

    @Transactional
    public ProjectDetail saveManualVersion(
        String ownerId,
        String projectId,
        Map<String, String> files,
        String summary
    ) {
        ProjectEntity project = requireProject(ownerId, projectId);
        ensureNoActiveRun(projectId);
        projectFiles.validate(files);
        String normalizedSummary = summary == null || summary.isBlank() ? "手动保存代码修改" : summary.trim();
        createVersion(project, files, normalizedSummary, "manual", null, normalizedSummary);
        messageRepository.save(new MessageEntity(
            UUID.randomUUID().toString(), projectId, "assistant", normalizedSummary,
            null, "succeeded", Instant.now()));
        return toDetail(project);
    }

    @Transactional(readOnly = true)
    public VersionDetail getVersion(String ownerId, String projectId, String versionId) {
        requireProject(ownerId, projectId);
        return toVersionDetail(requireVersion(projectId, versionId));
    }

    @Transactional
    public ProjectDetail restore(String ownerId, String projectId, String versionId) {
        ProjectEntity project = requireProject(ownerId, projectId);
        ensureNoActiveRun(projectId);
        VersionEntity source = requireVersion(projectId, versionId);
        Map<String, String> files = filesOf(source);
        String prompt = "恢复自版本 v" + source.getVersionNumber();
        createVersion(project, files, prompt, "restore", null, prompt);
        messageRepository.save(new MessageEntity(
            UUID.randomUUID().toString(), projectId, "assistant", prompt + "，历史版本保持不变。",
            null, "succeeded", Instant.now()));
        return toDetail(project);
    }

    @Transactional(readOnly = true)
    public Map<String, String> currentFiles(String ownerId, String projectId) {
        return currentFiles(requireProject(ownerId, projectId));
    }

    @Transactional
    public int recoverInterruptedRuns() {
        Instant now = Instant.now();
        List<GenerationRunEntity> interrupted = runRepository.findAllByStatusIn(ACTIVE_RUN_STATUSES);
        interrupted.forEach(run -> {
            String error = "服务重启中断了本次生成，当前版本未被修改，请直接重试";
            long durationMs = Math.max(0, java.time.Duration.between(run.getCreatedAt(), now).toMillis());
            run.fail(error, durationMs);
            runRepository.save(run);
            messageRepository.save(new MessageEntity(
                UUID.randomUUID().toString(), run.getProjectId(), "assistant",
                error + "。", run.getId(), "failed", now));
        });
        return interrupted.size();
    }

    private VersionEntity createVersion(
        ProjectEntity project,
        Map<String, String> files,
        String prompt,
        String source,
        String runId,
        String summary
    ) {
        int versionNumber = versionRepository.findFirstByProjectIdOrderByVersionNumberDesc(project.getId())
            .map(version -> version.getVersionNumber() + 1)
            .orElse(1);
        VersionEntity version = new VersionEntity(
            UUID.randomUUID().toString(), project.getId(), versionNumber, writeFiles(files), prompt,
            source, runId, truncate(summary, 500), Instant.now());
        versionRepository.save(version);
        project.applyVersion(version.getId());
        projectRepository.save(project);
        return version;
    }

    private void ensureNoActiveRun(String projectId) {
        runRepository.findFirstByProjectIdAndStatusIn(projectId, ACTIVE_RUN_STATUSES).ifPresent(run -> {
            throw new IllegalStateException("当前项目正在生成，暂时不能修改版本");
        });
    }

    private ProjectEntity requireProject(String ownerId, String projectId) {
        return projectRepository.findByIdAndGuestId(projectId, ownerId)
            .orElseThrow(() -> new NoSuchElementException("Project not found"));
    }

    private GenerationRunEntity requireRun(String projectId, String runId) {
        return runRepository.findByIdAndProjectId(runId, projectId)
            .orElseThrow(() -> new NoSuchElementException("Generation run not found"));
    }

    private VersionEntity requireVersion(String projectId, String versionId) {
        return versionRepository.findByIdAndProjectId(versionId, projectId)
            .orElseThrow(() -> new NoSuchElementException("Version not found"));
    }

    private Map<String, String> currentFiles(ProjectEntity project) {
        if (project.getCurrentVersionId() == null || project.getCurrentVersionId().isBlank()) {
            return Map.of();
        }
        return versionRepository.findByIdAndProjectId(project.getCurrentVersionId(), project.getId())
            .map(this::filesOf)
            .orElse(Map.of());
    }

    private Map<String, String> filesOf(VersionEntity version) {
        if (version.getFilesJson() == null || version.getFilesJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(version.getFilesJson(), new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("Stored project files are invalid", exception);
        }
    }

    private String writeFiles(Map<String, String> files) {
        try {
            return objectMapper.writeValueAsString(files);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to store project files", exception);
        }
    }

    private ProjectSummary toSummary(ProjectEntity project) {
        return new ProjectSummary(
            project.getId(), project.getName(), project.getCurrentVersionId() != null,
            project.getCreatedAt(), project.getUpdatedAt());
    }

    private ProjectDetail toDetail(ProjectEntity project) {
        return toDetail(
            project,
            messageRepository.findAllByProjectIdOrderByCreatedAtAsc(project.getId()),
            versionRepository.findAllByProjectIdOrderByVersionNumberDesc(project.getId()),
            runRepository.findAllByProjectIdOrderByCreatedAtAsc(project.getId())
        );
    }

    private ProjectDetail toDetail(
        ProjectEntity project,
        List<MessageEntity> messageEntities,
        List<VersionEntity> versionEntities,
        List<GenerationRunEntity> runEntities
    ) {
        List<MessageResponse> messages = messageEntities.stream()
            .map(message -> new MessageResponse(
                message.getId(), message.getRole(), message.getContent(), message.getRunId(), message.getStatus(),
                message.getCreatedAt()))
            .toList();
        List<VersionResponse> versions = versionEntities.stream()
            .map(this::toVersionResponse)
            .toList();
        List<GenerationRunResponse> runs = runEntities.stream()
            .map(this::toRunResponse)
            .toList();
        return new ProjectDetail(
            project.getId(), project.getName(), project.getCurrentVersionId(), currentFiles(project, versionEntities),
            project.getCreatedAt(), project.getUpdatedAt(), messages, versions, runs);
    }

    private Map<String, String> currentFiles(ProjectEntity project, List<VersionEntity> versions) {
        if (project.getCurrentVersionId() == null || project.getCurrentVersionId().isBlank()) {
            return Map.of();
        }
        return versions.stream()
            .filter(version -> version.getId().equals(project.getCurrentVersionId()))
            .findFirst()
            .map(this::filesOf)
            .orElse(Map.of());
    }

    private VersionResponse toVersionResponse(VersionEntity version) {
        return new VersionResponse(
            version.getId(), version.getVersionNumber(), version.getPrompt(),
            defaultString(version.getSource(), "legacy"), defaultString(version.getSummary(), version.getPrompt()),
            filesOf(version).size(), version.getCreatedAt());
    }

    private VersionDetail toVersionDetail(VersionEntity version) {
        return new VersionDetail(
            version.getId(), version.getVersionNumber(), version.getPrompt(),
            defaultString(version.getSource(), "legacy"), defaultString(version.getSummary(), version.getPrompt()),
            filesOf(version), version.getCreatedAt());
    }

    private GenerationRunResponse toRunResponse(GenerationRunEntity run) {
        return new GenerationRunResponse(
            run.getId(), run.getPrompt(), run.getStatus().toLowerCase(), run.getModel(), run.getAttemptCount(),
            run.getErrorMessage(), run.getDurationMs(), run.getCreatedAt(), run.getUpdatedAt());
    }

    private String conciseError(String value) {
        String message = value == null || value.isBlank() ? "Generation failed" : value.replaceAll("\\s+", " ").trim();
        return truncate(message, 800);
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
