package dev.buildtrace.project;

import dev.buildtrace.project.ProjectDtos.MessageResponse;
import dev.buildtrace.project.ProjectDtos.ProjectDetail;
import dev.buildtrace.project.ProjectDtos.ProjectSummary;
import dev.buildtrace.project.ProjectDtos.VersionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MessageRepository messageRepository;
    private final VersionRepository versionRepository;

    public ProjectService(
        ProjectRepository projectRepository,
        MessageRepository messageRepository,
        VersionRepository versionRepository
    ) {
        this.projectRepository = projectRepository;
        this.messageRepository = messageRepository;
        this.versionRepository = versionRepository;
    }

    @Transactional
    public ProjectDetail create(String guestId, String name) {
        requireGuestId(guestId);
        Instant now = Instant.now();
        ProjectEntity project = new ProjectEntity(UUID.randomUUID().toString(), guestId, name.trim(), now);
        projectRepository.save(project);
        return toDetail(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> list(String guestId) {
        requireGuestId(guestId);
        return projectRepository.findAllByGuestIdOrderByUpdatedAtDesc(guestId).stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetail get(String guestId, String projectId) {
        return toDetail(requireProject(guestId, projectId));
    }

    @Transactional
    public void addUserMessage(String guestId, String projectId, String prompt) {
        ProjectEntity project = requireProject(guestId, projectId);
        messageRepository.save(new MessageEntity(
            UUID.randomUUID().toString(),
            project.getId(),
            "user",
            prompt,
            Instant.now()
        ));
    }

    @Transactional(readOnly = true)
    public String currentHtml(String guestId, String projectId) {
        return requireProject(guestId, projectId).getCurrentHtml();
    }

    @Transactional
    public ProjectDetail completeGeneration(String guestId, String projectId, String prompt, String html) {
        ProjectEntity project = requireProject(guestId, projectId);
        int versionNumber = versionRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId)
            .map(version -> version.getVersionNumber() + 1)
            .orElse(1);

        VersionEntity version = new VersionEntity(
            UUID.randomUUID().toString(),
            projectId,
            versionNumber,
            html,
            prompt,
            Instant.now()
        );
        versionRepository.save(version);
        project.applyHtml(html);
        projectRepository.save(project);
        messageRepository.save(new MessageEntity(
            UUID.randomUUID().toString(),
            projectId,
            "assistant",
            "已生成版本 v" + versionNumber + "，可以在右侧预览并继续修改。",
            Instant.now()
        ));
        return toDetail(project);
    }

    @Transactional
    public ProjectDetail restore(String guestId, String projectId, String versionId) {
        ProjectEntity project = requireProject(guestId, projectId);
        VersionEntity source = versionRepository.findByIdAndProjectId(versionId, projectId)
            .orElseThrow(() -> new NoSuchElementException("Version not found"));

        int nextVersion = versionRepository.findFirstByProjectIdOrderByVersionNumberDesc(projectId)
            .map(version -> version.getVersionNumber() + 1)
            .orElse(1);
        String prompt = "恢复自版本 v" + source.getVersionNumber();
        versionRepository.save(new VersionEntity(
            UUID.randomUUID().toString(),
            projectId,
            nextVersion,
            source.getHtml(),
            prompt,
            Instant.now()
        ));
        project.applyHtml(source.getHtml());
        projectRepository.save(project);
        messageRepository.save(new MessageEntity(
            UUID.randomUUID().toString(),
            projectId,
            "assistant",
            "已将版本 v" + source.getVersionNumber() + " 恢复为新的 v" + nextVersion + "。",
            Instant.now()
        ));
        return toDetail(project);
    }

    private ProjectEntity requireProject(String guestId, String projectId) {
        requireGuestId(guestId);
        return projectRepository.findByIdAndGuestId(projectId, guestId)
            .orElseThrow(() -> new NoSuchElementException("Project not found"));
    }

    private void requireGuestId(String guestId) {
        if (guestId == null || guestId.isBlank() || guestId.length() > 128) {
            throw new IllegalArgumentException("X-Guest-Id is required");
        }
    }

    private ProjectSummary toSummary(ProjectEntity project) {
        return new ProjectSummary(
            project.getId(),
            project.getName(),
            project.getCurrentHtml() != null && !project.getCurrentHtml().isBlank(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }

    private ProjectDetail toDetail(ProjectEntity project) {
        List<MessageResponse> messages = messageRepository.findAllByProjectIdOrderByCreatedAtAsc(project.getId()).stream()
            .map(message -> new MessageResponse(
                message.getId(), message.getRole(), message.getContent(), message.getCreatedAt()))
            .toList();
        List<VersionResponse> versions = versionRepository.findAllByProjectIdOrderByVersionNumberDesc(project.getId()).stream()
            .map(version -> new VersionResponse(
                version.getId(), version.getVersionNumber(), version.getPrompt(), version.getCreatedAt()))
            .toList();
        return new ProjectDetail(
            project.getId(),
            project.getName(),
            project.getCurrentHtml(),
            project.getCreatedAt(),
            project.getUpdatedAt(),
            messages,
            versions
        );
    }
}
