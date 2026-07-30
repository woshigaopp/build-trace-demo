package dev.buildtrace.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "project_versions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_project_version", columnNames = {"projectId", "versionNumber"})
})
public class VersionEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private int versionNumber;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String html;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String filesJson;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(length = 24)
    private String source;

    @Column(length = 36)
    private String generationRunId;

    @Column(length = 500)
    private String summary;

    @Column(nullable = false)
    private Instant createdAt;

    protected VersionEntity() {
    }

    public VersionEntity(
        String id,
        String projectId,
        int versionNumber,
        String html,
        String prompt,
        Instant createdAt
    ) {
        this.id = id;
        this.projectId = projectId;
        this.versionNumber = versionNumber;
        this.html = html;
        this.prompt = prompt;
        this.createdAt = createdAt;
    }

    public VersionEntity(
        String id,
        String projectId,
        int versionNumber,
        String filesJson,
        String prompt,
        String source,
        String generationRunId,
        String summary,
        Instant createdAt
    ) {
        this.id = id;
        this.projectId = projectId;
        this.versionNumber = versionNumber;
        this.html = "";
        this.filesJson = filesJson;
        this.prompt = prompt;
        this.source = source;
        this.generationRunId = generationRunId;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getProjectId() {
        return projectId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getHtml() {
        return html;
    }

    public String getFilesJson() {
        return filesJson;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getSource() {
        return source;
    }

    public String getGenerationRunId() {
        return generationRunId;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
