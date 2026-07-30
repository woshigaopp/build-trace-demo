package dev.buildtrace.project;

import dev.buildtrace.generation.GenerationService;
import dev.buildtrace.auth.AuthenticatedUser;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final GenerationService generationService;

    public ProjectController(ProjectService projectService, GenerationService generationService) {
        this.projectService = projectService;
        this.generationService = generationService;
    }

    @PostMapping
    ProjectDetail create(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody CreateProjectRequest request
    ) {
        return projectService.create(user.id(), request.name());
    }

    @GetMapping
    List<ProjectSummary> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return projectService.list(user.id());
    }

    @GetMapping("/{projectId}")
    ProjectDetail get(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String projectId
    ) {
        return projectService.get(user.id(), projectId);
    }

    @PostMapping(path = "/{projectId}/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter generate(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String projectId,
        @Valid @RequestBody GenerateRequest request
    ) {
        return generationService.generate(user.id(), projectId, request.prompt());
    }

    @PostMapping("/{projectId}/versions")
    ProjectDetail saveVersion(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String projectId,
        @Valid @RequestBody ProjectDtos.SaveVersionRequest request
    ) {
        return projectService.saveManualVersion(user.id(), projectId, request.files(), request.summary());
    }

    @GetMapping("/{projectId}/versions/{versionId}")
    ProjectDtos.VersionDetail getVersion(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String projectId,
        @PathVariable String versionId
    ) {
        return projectService.getVersion(user.id(), projectId, versionId);
    }

    @PostMapping("/{projectId}/versions/{versionId}/restore")
    ProjectDetail restore(
        @AuthenticationPrincipal AuthenticatedUser user,
        @PathVariable String projectId,
        @PathVariable String versionId
    ) {
        return projectService.restore(user.id(), projectId, versionId);
    }
}
