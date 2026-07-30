package dev.buildtrace.generation;

import dev.buildtrace.project.ProjectDtos.ProjectDetail;
import dev.buildtrace.project.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:generation-lifecycle-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GenerationLifecycleIntegrationTest {

    @Autowired
    private GenerationService generationService;

    @Autowired
    private ProjectService projectService;

    @MockitoBean
    private AiGenerationClient aiClient;

    @BeforeEach
    void configureModel() {
        reset(aiClient);
        when(aiClient.configured()).thenReturn(true);
        when(aiClient.model()).thenReturn("deterministic-test-model");
    }

    @Test
    void completesFiveSerialIncrementalOperationsWithDurableHistory() {
        String owner = UUID.randomUUID().toString();
        ProjectDetail project = projectService.create(owner, "Five increments");
        AtomicInteger response = new AtomicInteger();
        when(aiClient.stream(anyString(), anyMap())).thenAnswer(invocation ->
            Flux.just(validResponse(response.incrementAndGet())));

        for (int step = 1; step <= 5; step++) {
            generationService.generate(owner, project.id(), "increment " + step);
            project = awaitTerminal(owner, project.id(), step);
            assertThat(project.runs().getLast().status()).isEqualTo("succeeded");
        }

        assertThat(project.versions()).hasSize(5);
        assertThat(project.messages()).hasSize(10);
        assertThat(project.runs()).allMatch(run -> run.status().equals("succeeded"));
        assertThat(project.currentFiles().get("/App.jsx")).contains("Version 5");
    }

    @Test
    void repairsOneInvalidResponseAndPublishesOnlyValidatedCandidate() {
        String owner = UUID.randomUUID().toString();
        ProjectDetail created = projectService.create(owner, "Repair succeeds");
        when(aiClient.stream(anyString(), anyMap())).thenReturn(Flux.just("not-json"));
        when(aiClient.repair(anyString(), anyMap(), anyString(), anyString()))
            .thenReturn(Flux.just(validResponse(1)));

        generationService.generate(owner, created.id(), "make it valid");
        ProjectDetail completed = awaitTerminal(owner, created.id(), 1);

        assertThat(completed.runs().getFirst().status()).isEqualTo("succeeded");
        assertThat(completed.runs().getFirst().attemptCount()).isEqualTo(2);
        assertThat(completed.versions()).hasSize(1);
        assertThat(completed.currentFiles().get("/App.jsx")).contains("Version 1");
    }

    @Test
    void failsAfterOneRepairAndKeepsCurrentVersionAtomic() {
        String owner = UUID.randomUUID().toString();
        ProjectDetail created = projectService.create(owner, "Repair fails");
        when(aiClient.stream(anyString(), anyMap())).thenReturn(Flux.just(validResponse(1)));
        generationService.generate(owner, created.id(), "create stable version");
        ProjectDetail stable = awaitTerminal(owner, created.id(), 1);

        when(aiClient.stream(anyString(), anyMap())).thenReturn(Flux.just("not-json"));
        when(aiClient.repair(anyString(), anyMap(), anyString(), anyString()))
            .thenReturn(Flux.just("still-not-json"));
        generationService.generate(owner, created.id(), "break current version");
        ProjectDetail failed = awaitTerminal(owner, created.id(), 2);

        assertThat(failed.runs().getLast().status()).isEqualTo("failed");
        assertThat(failed.runs().getLast().attemptCount()).isEqualTo(2);
        assertThat(failed.currentVersionId()).isEqualTo(stable.currentVersionId());
        assertThat(failed.currentFiles()).isEqualTo(stable.currentFiles());
        assertThat(failed.versions()).hasSize(1);
        assertThat(failed.messages().getLast().status()).isEqualTo("failed");
    }

    private ProjectDetail awaitTerminal(String owner, String projectId, int expectedRuns) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            ProjectDetail project = projectService.get(owner, projectId);
            if (project.runs().size() == expectedRuns
                && !active(project.runs().getLast().status())) {
                return project;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for generation", exception);
            }
        }
        throw new AssertionError("Generation did not reach a terminal state");
    }

    private boolean active(String status) {
        return status.equals("queued") || status.equals("generating")
            || status.equals("validating") || status.equals("repairing");
    }

    private String validResponse(int version) {
        return """
            {"summary":"version %1$d","operations":[{"type":"write","path":"/App.jsx","content":"import React from 'react'; export default function App() { return <main><h1>Version %1$d</h1><button onClick={() => alert('ok')}>Run</button></main>; }"}]}
            """.formatted(version).trim();
    }
}
