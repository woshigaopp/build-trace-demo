package dev.buildtrace.project;

import dev.buildtrace.generation.ProjectFiles;
import dev.buildtrace.project.ProjectDtos.GenerationContext;
import dev.buildtrace.project.ProjectDtos.ProjectDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:project-service-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProjectServiceIntegrationTest {

    private static final String OWNER_ID = "integration-test-owner";

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectFiles projectFiles;

    @Test
    void persistsRunsMessagesSnapshotsAndRestoresWithoutOverwritingHistory() {
        ProjectDetail created = projectService.create(OWNER_ID, "  Versioned app  ");

        GenerationContext firstRun = projectService.beginGeneration(
            OWNER_ID, created.id(), "create version one", "test-model");
        Map<String, String> versionOneFiles = withApp(projectFiles.starter(), "Version one");
        ProjectDetail versionOne = projectService.completeGeneration(
            OWNER_ID, created.id(), firstRun.runId(), firstRun.prompt(), versionOneFiles, "first", 10);

        GenerationContext secondRun = projectService.beginGeneration(
            OWNER_ID, created.id(), "create version two", "test-model");
        Map<String, String> versionTwoFiles = withApp(versionOneFiles, "Version two");
        ProjectDetail versionTwo = projectService.completeGeneration(
            OWNER_ID, created.id(), secondRun.runId(), secondRun.prompt(), versionTwoFiles, "second", 12);

        String versionOneId = versionTwo.versions().stream()
            .filter(version -> version.versionNumber() == 1)
            .findFirst().orElseThrow().id();
        ProjectDetail restored = projectService.restore(OWNER_ID, created.id(), versionOneId);
        ProjectDetail reloaded = projectService.get(OWNER_ID, created.id());

        assertThat(created.name()).isEqualTo("Versioned app");
        assertThat(versionOne.versions()).extracting(ProjectDtos.VersionResponse::versionNumber).containsExactly(1);
        assertThat(versionTwo.versions()).extracting(ProjectDtos.VersionResponse::versionNumber).containsExactly(2, 1);
        assertThat(restored.currentFiles().get("/App.jsx")).contains("Version one");
        assertThat(restored.versions()).extracting(ProjectDtos.VersionResponse::versionNumber).containsExactly(3, 2, 1);
        assertThat(reloaded.messages()).hasSize(5);
        assertThat(reloaded.runs()).extracting(ProjectDtos.GenerationRunResponse::status)
            .containsExactly("succeeded", "succeeded");
    }

    @Test
    void failedRunPersistsWithoutChangingCurrentSnapshot() {
        ProjectDetail created = projectService.create(OWNER_ID, "Atomic failure");
        GenerationContext first = projectService.beginGeneration(OWNER_ID, created.id(), "initial", "test-model");
        ProjectDetail succeeded = projectService.completeGeneration(
            OWNER_ID, created.id(), first.runId(), first.prompt(), withApp(projectFiles.starter(), "Stable"),
            "stable", 5);

        GenerationContext failed = projectService.beginGeneration(OWNER_ID, created.id(), "break it", "test-model");
        projectService.transitionRun(OWNER_ID, created.id(), failed.runId(), GenerationRunStatus.REPAIRING, 2);
        ProjectDetail afterFailure = projectService.failGeneration(
            OWNER_ID, created.id(), failed.runId(), "invalid response", 9);

        assertThat(afterFailure.currentVersionId()).isEqualTo(succeeded.currentVersionId());
        assertThat(afterFailure.currentFiles()).isEqualTo(succeeded.currentFiles());
        assertThat(afterFailure.versions()).hasSize(1);
        assertThat(afterFailure.runs().getLast().status()).isEqualTo("failed");
        assertThat(afterFailure.runs().getLast().attemptCount()).isEqualTo(2);
        assertThat(afterFailure.messages().getLast().status()).isEqualTo("failed");
    }

    @Test
    void isolatesProjectsByAuthenticatedOwner() {
        ProjectDetail created = projectService.create(OWNER_ID, "Private project");

        assertThatThrownBy(() -> projectService.get("another-owner", created.id()))
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessage("Project not found");
        assertThat(projectService.list("another-owner")).isEmpty();
    }

    @Test
    void recoversInterruptedRunsAsRetryableFailuresWithoutChangingVersion() {
        ProjectDetail created = projectService.create(OWNER_ID, "Interrupted generation");
        GenerationContext initial = projectService.beginGeneration(
            OWNER_ID, created.id(), "create stable version", "test-model");
        ProjectDetail stable = projectService.completeGeneration(
            OWNER_ID, created.id(), initial.runId(), initial.prompt(),
            withApp(projectFiles.starter(), "Stable before restart"), "stable", 5);
        GenerationContext interrupted = projectService.beginGeneration(
            OWNER_ID, created.id(), "unfinished request", "test-model");
        projectService.transitionRun(
            OWNER_ID, created.id(), interrupted.runId(), GenerationRunStatus.GENERATING, 1);

        int recovered = projectService.recoverInterruptedRuns();
        ProjectDetail afterRestart = projectService.get(OWNER_ID, created.id());

        assertThat(recovered).isEqualTo(1);
        assertThat(afterRestart.currentVersionId()).isEqualTo(stable.currentVersionId());
        assertThat(afterRestart.versions()).hasSize(1);
        assertThat(afterRestart.runs().getLast().status()).isEqualTo("failed");
        assertThat(afterRestart.runs().getLast().errorMessage()).contains("服务重启");
        assertThat(afterRestart.messages().getLast().status()).isEqualTo("failed");
        assertThat(afterRestart.messages().getLast().content()).contains("请直接重试");
    }

    private Map<String, String> withApp(Map<String, String> source, String heading) {
        Map<String, String> files = new LinkedHashMap<>(source);
        files.put("/App.jsx", """
            import React from 'react';
            export default function App() { return <h1>%s</h1>; }
            """.formatted(heading));
        return files;
    }
}
