package dev.buildtrace.project;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GenerationRunRecovery {

    private final ProjectService projectService;

    public GenerationRunRecovery(ProjectService projectService) {
        this.projectService = projectService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverAfterRestart() {
        projectService.recoverInterruptedRuns();
    }
}
