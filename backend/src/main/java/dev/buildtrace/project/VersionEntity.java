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
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

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

    public String getPrompt() {
        return prompt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
