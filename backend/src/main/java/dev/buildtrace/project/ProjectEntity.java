package dev.buildtrace.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "projects")
public class ProjectEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 128)
    private String guestId;

    @Column(nullable = false, length = 160)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String currentHtml;

    @Column(length = 36)
    private String currentVersionId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ProjectEntity() {
    }

    public ProjectEntity(String id, String ownerId, String name, Instant createdAt) {
        this.id = id;
        this.guestId = ownerId;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return guestId;
    }

    public String getName() {
        return name;
    }

    public String getCurrentHtml() {
        return currentHtml;
    }

    public String getCurrentVersionId() {
        return currentVersionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void applyVersion(String versionId) {
        this.currentVersionId = versionId;
        this.currentHtml = "";
        this.updatedAt = Instant.now();
    }
}
