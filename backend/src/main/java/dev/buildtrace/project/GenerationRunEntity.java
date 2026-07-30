package dev.buildtrace.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "generation_runs")
public class GenerationRunEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String projectId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(nullable = false, length = 160)
    private String model;

    @Column(nullable = false)
    private int attemptCount;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column
    private Long durationMs;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected GenerationRunEntity() {
    }

    public GenerationRunEntity(String id, String projectId, String prompt, String model, Instant now) {
        this.id = id;
        this.projectId = projectId;
        this.prompt = prompt;
        this.model = model;
        this.status = GenerationRunStatus.QUEUED.name();
        this.attemptCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void transition(GenerationRunStatus status, int attemptCount) {
        this.status = status.name();
        this.attemptCount = attemptCount;
        this.updatedAt = Instant.now();
    }

    public void succeed(long durationMs) {
        this.status = GenerationRunStatus.SUCCEEDED.name();
        this.durationMs = durationMs;
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }

    public void fail(String errorMessage, long durationMs) {
        this.status = GenerationRunStatus.FAILED.name();
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getProjectId() { return projectId; }
    public String getPrompt() { return prompt; }
    public String getStatus() { return status; }
    public String getModel() { return model; }
    public int getAttemptCount() { return attemptCount; }
    public String getErrorMessage() { return errorMessage; }
    public Long getDurationMs() { return durationMs; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
