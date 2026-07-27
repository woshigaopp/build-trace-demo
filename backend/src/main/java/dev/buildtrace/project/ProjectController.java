package dev.buildtrace.project;

import dev.buildtrace.generation.GenerationService;
import dev.buildtrace.project.ProjectDtos.CreateProjectRequest;
import dev.buildtrace.project.ProjectDtos.GenerateRequest;
import dev.buildtrace.project.ProjectDtos.ProjectDetail;
import dev.buildtrace.project.ProjectDtos.ProjectSummary;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final String GUEST_HEADER = "X-Guest-Id";

    private final ProjectService projectService;
    private final GenerationService generationService;

    public ProjectController(ProjectService projectService, GenerationService generationService) {
        this.projectService = projectService;
        this.generationService = generationService;
    }

    @PostMapping
    ProjectDetail create(
        @RequestHeader(GUEST_HEADER) String guestId,
        @Valid @RequestBody CreateProjectRequest request
    ) {
        return projectService.create(guestId, request.name());
    }

    @GetMapping
    List<ProjectSummary> list(@RequestHeader(GUEST_HEADER) String guestId) {
        return projectService.list(guestId);
    }

    @GetMapping("/{projectId}")
    ProjectDetail get(
        @RequestHeader(GUEST_HEADER) String guestId,
        @PathVariable String projectId
    ) {
        return projectService.get(guestId, projectId);
    }

    @PostMapping(path = "/{projectId}/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter generate(
        @RequestHeader(GUEST_HEADER) String guestId,
        @PathVariable String projectId,
        @Valid @RequestBody GenerateRequest request
    ) {
        return generationService.generate(guestId, projectId, request.prompt());
    }

    @PostMapping("/{projectId}/versions/{versionId}/restore")
    ProjectDetail restore(
        @RequestHeader(GUEST_HEADER) String guestId,
        @PathVariable String projectId,
        @PathVariable String versionId
    ) {
        return projectService.restore(guestId, projectId, versionId);
    }
}
