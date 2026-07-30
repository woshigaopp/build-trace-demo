package dev.buildtrace.project;

import dev.buildtrace.project.ProjectDtos.PublishedProjectResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/projects")
public class PublicProjectController {

    private final ProjectService projectService;

    public PublicProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/{token}")
    PublishedProjectResponse get(@PathVariable String token) {
        return projectService.getPublished(token);
    }
}
