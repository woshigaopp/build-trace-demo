package dev.buildtrace.project;

import dev.buildtrace.project.ProjectDtos.ProjectDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:project-service-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ProjectServiceIntegrationTest {

    private static final String GUEST_ID = "integration-test-guest";
    private static final String HTML_V1 = """
        <!DOCTYPE html><html><head><title>Version 1</title></head><body>
        <main><h1>First generated version</h1><p>This complete document is persisted for integration testing.</p></main>
        </body></html>
        """;
    private static final String HTML_V2 = """
        <!DOCTYPE html><html><head><title>Version 2</title></head><body>
        <main><h1>Second generated version</h1><p>This document verifies version progression and restoration.</p></main>
        </body></html>
        """;

    @Autowired
    private ProjectService projectService;

    @Test
    void persistsMessagesVersionsAndRestoresWithoutOverwritingHistory() {
        ProjectDetail created = projectService.create(GUEST_ID, "  Versioned app  ");

        projectService.addUserMessage(GUEST_ID, created.id(), "create version one");
        ProjectDetail versionOne = projectService.completeGeneration(
            GUEST_ID, created.id(), "create version one", HTML_V1);
        projectService.addUserMessage(GUEST_ID, created.id(), "create version two");
        ProjectDetail versionTwo = projectService.completeGeneration(
            GUEST_ID, created.id(), "create version two", HTML_V2);

        ProjectDetail restored = projectService.restore(
            GUEST_ID, created.id(), versionOne.versions().getFirst().id());
        ProjectDetail reloaded = projectService.get(GUEST_ID, created.id());

        assertThat(created.name()).isEqualTo("Versioned app");
        assertThat(versionOne.versions()).extracting(ProjectDtos.VersionResponse::versionNumber)
            .containsExactly(1);
        assertThat(versionTwo.versions()).extracting(ProjectDtos.VersionResponse::versionNumber)
            .containsExactly(2, 1);
        assertThat(restored.currentHtml()).isEqualTo(HTML_V1);
        assertThat(restored.versions()).extracting(ProjectDtos.VersionResponse::versionNumber)
            .containsExactly(3, 2, 1);
        assertThat(restored.versions().getFirst().prompt()).isEqualTo("恢复自版本 v1");
        assertThat(reloaded.messages()).hasSize(5);
        assertThat(reloaded.updatedAt()).isAfterOrEqualTo(Instant.parse(created.updatedAt().toString()));
    }

    @Test
    void isolatesProjectsByGuestId() {
        ProjectDetail created = projectService.create(GUEST_ID, "Private project");

        assertThatThrownBy(() -> projectService.get("another-guest", created.id()))
            .isInstanceOf(java.util.NoSuchElementException.class)
            .hasMessage("Project not found");
        assertThat(projectService.list("another-guest")).isEmpty();
    }
}
