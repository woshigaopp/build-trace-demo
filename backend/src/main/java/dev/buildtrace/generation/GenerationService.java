package dev.buildtrace.generation;

import dev.buildtrace.project.GenerationRunStatus;
import dev.buildtrace.project.ProjectDtos.GenerationContext;
import dev.buildtrace.project.ProjectDtos.ProjectDetail;
import dev.buildtrace.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Service
public class GenerationService {

    private static final long SSE_TIMEOUT_MILLIS = Duration.ofMinutes(4).toMillis();

    private final ProjectService projectService;
    private final AiGenerationClient aiClient;
    private final FallbackGenerator fallbackGenerator;
    private final StructuredOutputParser outputParser;
    private final ProjectFiles projectFiles;
    private final ExecutorService executor;

    public GenerationService(
        ProjectService projectService,
        AiGenerationClient aiClient,
        FallbackGenerator fallbackGenerator,
        StructuredOutputParser outputParser,
        ProjectFiles projectFiles,
        ExecutorService executor
    ) {
        this.projectService = projectService;
        this.aiClient = aiClient;
        this.fallbackGenerator = fallbackGenerator;
        this.outputParser = outputParser;
        this.projectFiles = projectFiles;
        this.executor = executor;
    }

    public SseEmitter generate(String ownerId, String projectId, String prompt) {
        String model = aiClient.configured() ? aiClient.model() : "local-fallback";
        GenerationContext context = projectService.beginGeneration(ownerId, projectId, prompt, model);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        executor.submit(() -> run(emitter, ownerId, context));
        return emitter;
    }

    private void run(SseEmitter emitter, String ownerId, GenerationContext context) {
        long startedAt = System.nanoTime();
        String rawOutput = "";
        try {
            transition(emitter, ownerId, context, GenerationRunStatus.GENERATING, 1,
                context.currentFiles().isEmpty() ? "正在创建多文件 React 应用" : "正在生成增量文件操作");

            GenerationResult result;
            if (!aiClient.configured()) {
                result = fallbackGenerator.generate(context.prompt());
                projectService.recordRunPlan(
                    ownerId, context.projectId(), context.runId(), result.understanding(), result.plan());
                sendSafely(emitter, "phase", Map.of("step", "fallback", "message", "本地环境未配置模型，使用明确标记的交互式 fallback"));
            } else {
                rawOutput = collect(aiClient.stream(context.prompt(), context.currentFiles()), emitter);
                transition(emitter, ownerId, context, GenerationRunStatus.VALIDATING, 1,
                    "正在校验文件操作与 React 项目结构");
                try {
                    result = outputParser.parse(rawOutput);
                    projectService.recordRunPlan(
                        ownerId, context.projectId(), context.runId(), result.understanding(), result.plan());
                    projectFiles.candidate(context.currentFiles(), result);
                } catch (RuntimeException firstError) {
                    transition(emitter, ownerId, context, GenerationRunStatus.REPAIRING, 2,
                        "首次结果未通过校验，正在自动修复一次");
                    rawOutput = collect(aiClient.repair(
                        context.prompt(), context.currentFiles(), rawOutput, firstError.getMessage()), emitter);
                    transition(emitter, ownerId, context, GenerationRunStatus.VALIDATING, 2,
                        "正在校验修复后的候选版本");
                    result = outputParser.parse(rawOutput);
                    projectService.recordRunPlan(
                        ownerId, context.projectId(), context.runId(), result.understanding(), result.plan());
                }
            }

            Map<String, String> candidate = projectFiles.candidate(context.currentFiles(), result);
            List<String> changedFiles = changedPaths(context.currentFiles(), candidate);
            List<String> checks = List.of(
                "结构化文件操作协议已解析",
                "文件路径、数量和内容大小符合安全边界",
                "Vite React 必需脚手架与入口完整",
                "候选快照通过原子版本校验"
            );
            long durationMs = elapsed(startedAt);
            ProjectDetail project = projectService.completeGeneration(
                ownerId, context.projectId(), context.runId(), context.prompt(), candidate,
                result.summary(), result.understanding(), result.plan(), changedFiles, checks, durationMs);
            sendSafely(emitter, "completed", Map.of(
                "project", project,
                "runId", context.runId(),
                "fallback", !aiClient.configured(),
                "model", aiClient.configured() ? aiClient.model() : "local-fallback",
                "durationMs", durationMs
            ));
            emitter.complete();
        } catch (Throwable error) {
            long durationMs = elapsed(startedAt);
            ProjectDetail project = projectService.failGeneration(
                ownerId, context.projectId(), context.runId(), rootMessage(error), durationMs);
            sendSafely(emitter, "generation-error", Map.of(
                "message", rootMessage(error), "runId", context.runId(), "project", project));
            emitter.complete();
        }
    }

    private String collect(reactor.core.publisher.Flux<String> stream, SseEmitter emitter) {
        StringBuilder output = new StringBuilder();
        stream.doOnNext(fragment -> {
            output.append(fragment);
            sendSafely(emitter, "token", Map.of("content", fragment));
        }).then().block();
        return output.toString();
    }

    private void transition(
        SseEmitter emitter,
        String ownerId,
        GenerationContext context,
        GenerationRunStatus status,
        int attempt,
        String message
    ) {
        projectService.transitionRun(ownerId, context.projectId(), context.runId(), status, attempt, message);
        sendSafely(emitter, "phase", Map.of(
            "step", status.name().toLowerCase(), "status", status.name().toLowerCase(), "message", message));
    }

    private long elapsed(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private List<String> changedPaths(Map<String, String> current, Map<String, String> candidate) {
        return java.util.stream.Stream.concat(current.keySet().stream(), candidate.keySet().stream())
            .distinct()
            .filter(path -> !java.util.Objects.equals(current.get(path), candidate.get(path)))
            .sorted()
            .toList();
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "Generation failed" : current.getMessage();
    }

    private void sendSafely(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException ignored) {
            // Generation deliberately continues because the durable run can be recovered after reconnect.
        }
    }
}
