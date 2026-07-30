package dev.buildtrace.project;

import dev.buildtrace.generation.ProjectFiles;
import dev.buildtrace.generation.ShowcaseProject;
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

    @Autowired
    private ShowcaseProject showcaseProject;

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

    @Test
    void persistsTruthfulBuildTraceMetadata() {
        ProjectDetail created = projectService.create(OWNER_ID, "Traceable build");
        GenerationContext run = projectService.beginGeneration(
            OWNER_ID, created.id(), "add candidate search", "trace-model");
        projectService.transitionRun(
            OWNER_ID, created.id(), run.runId(), GenerationRunStatus.GENERATING, 1,
            "正在生成最小必要文件操作");
        projectService.recordRunPlan(
            OWNER_ID, created.id(), run.runId(), "为候选人看板增加搜索", java.util.List.of("增加搜索状态", "过滤候选人列表"));
        Map<String, String> files = withApp(projectFiles.starter(), "Searchable candidates");
        projectService.completeGeneration(
            OWNER_ID, created.id(), run.runId(), run.prompt(), files, "搜索已交付",
            "为候选人看板增加搜索", java.util.List.of("增加搜索状态", "过滤候选人列表"),
            java.util.List.of("/App.jsx"), java.util.List.of("React 入口完整", "候选快照通过原子校验"), 42);

        ProjectDtos.GenerationRunResponse persisted = projectService.get(OWNER_ID, created.id()).runs().getFirst();

        assertThat(persisted.understanding()).isEqualTo("为候选人看板增加搜索");
        assertThat(persisted.plan()).containsExactly("增加搜索状态", "过滤候选人列表");
        assertThat(persisted.changedFiles()).containsExactly("/App.jsx");
        assertThat(persisted.checks()).containsExactly("React 入口完整", "候选快照通过原子校验");
        assertThat(persisted.deliveredVersionNumber()).isEqualTo(1);
        assertThat(persisted.trace()).extracting(ProjectDtos.TraceEventResponse::status)
            .containsExactly("queued", "generating", "succeeded");
    }

    @Test
    void publicationPinsImmutableVersionUntilExplicitRepublish() {
        ProjectDetail created = projectService.create(OWNER_ID, "Published app");
        GenerationContext first = projectService.beginGeneration(OWNER_ID, created.id(), "v1", "test-model");
        ProjectDetail versionOne = projectService.completeGeneration(
            OWNER_ID, created.id(), first.runId(), first.prompt(), withApp(projectFiles.starter(), "Version one"),
            "v1", 10);
        ProjectDetail firstPublication = projectService.publish(OWNER_ID, created.id());
        String token = firstPublication.publication().token();

        GenerationContext second = projectService.beginGeneration(OWNER_ID, created.id(), "v2", "test-model");
        projectService.completeGeneration(
            OWNER_ID, created.id(), second.runId(), second.prompt(), withApp(versionOne.currentFiles(), "Version two"),
            "v2", 10);

        assertThat(projectService.getPublished(token).versionNumber()).isEqualTo(1);
        assertThat(projectService.getPublished(token).files().get("/App.jsx")).contains("Version one");

        ProjectDetail secondPublication = projectService.publish(OWNER_ID, created.id());
        assertThat(secondPublication.publication().token()).isEqualTo(token);
        assertThat(secondPublication.publication().versionNumber()).isEqualTo(2);
        assertThat(projectService.getPublished(token).files().get("/App.jsx")).contains("Version two");
    }

    @Test
    void createsLabelledInteractiveShowcase() {
        ProjectDetail showcase = projectService.createShowcase(OWNER_ID, showcaseProject.files());

        assertThat(showcase.name()).contains("示例");
        assertThat(showcase.versions()).singleElement().satisfies(version -> {
            assertThat(version.source()).isEqualTo("template");
            assertThat(version.fileCount()).isGreaterThanOrEqualTo(8);
        });
        assertThat(showcase.currentFiles().get("/App.jsx")).contains("usePersistentState");
        assertThat(showcase.currentFiles().get("/components/CandidateCard.jsx")).contains("onAdvance");
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
