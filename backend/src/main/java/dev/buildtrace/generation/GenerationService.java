package dev.buildtrace.generation;

import dev.buildtrace.project.ProjectDtos.ProjectDetail;
import dev.buildtrace.project.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GenerationService {

    private static final long SSE_TIMEOUT_MILLIS = Duration.ofMinutes(3).toMillis();

    private final ProjectService projectService;
    private final AiGenerationClient aiClient;
    private final FallbackGenerator fallbackGenerator;
    private final HtmlExtractor htmlExtractor;
    private final ExecutorService executor;

    public GenerationService(
        ProjectService projectService,
        AiGenerationClient aiClient,
        FallbackGenerator fallbackGenerator,
        HtmlExtractor htmlExtractor,
        ExecutorService executor
    ) {
        this.projectService = projectService;
        this.aiClient = aiClient;
        this.fallbackGenerator = fallbackGenerator;
        this.htmlExtractor = htmlExtractor;
        this.executor = executor;
    }

    public SseEmitter generate(String guestId, String projectId, String prompt) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        executor.submit(() -> runGeneration(emitter, guestId, projectId, prompt));
        return emitter;
    }

    private void runGeneration(SseEmitter emitter, String guestId, String projectId, String prompt) {
        long startedAt = System.nanoTime();
        try {
            projectService.addUserMessage(guestId, projectId, prompt);
            String currentHtml = projectService.currentHtml(guestId, projectId);
            send(emitter, "phase", Map.of(
                "step", "context",
                "message", currentHtml == null ? "正在创建首个版本" : "正在读取当前版本并组装修改上下文"
            ));

            if (!aiClient.configured()) {
                runFallback(emitter, guestId, projectId, prompt, startedAt);
                return;
            }

            send(emitter, "phase", Map.of(
                "step", "model",
                "message", "正在调用 " + aiClient.model() + " 生成应用"
            ));
            StringBuilder output = new StringBuilder();
            AtomicBoolean terminated = new AtomicBoolean(false);

            aiClient.stream(prompt, currentHtml).subscribe(
                fragment -> {
                    output.append(fragment);
                    sendSafely(emitter, "token", Map.of("content", fragment));
                },
                error -> {
                    if (terminated.compareAndSet(false, true)) {
                        fail(emitter, error);
                    }
                },
                () -> {
                    if (terminated.compareAndSet(false, true)) {
                        complete(emitter, guestId, projectId, prompt, output.toString(), false, startedAt);
                    }
                }
            );
        } catch (Exception exception) {
            fail(emitter, exception);
        }
    }

    private void runFallback(
        SseEmitter emitter,
        String guestId,
        String projectId,
        String prompt,
        long startedAt
    ) {
        send(emitter, "phase", Map.of(
            "step", "fallback",
            "message", "未配置模型 Key，使用明确标记的本地交互式 fallback"
        ));
        String html = fallbackGenerator.generate(prompt);
        complete(emitter, guestId, projectId, prompt, html, true, startedAt);
    }

    private void complete(
        SseEmitter emitter,
        String guestId,
        String projectId,
        String prompt,
        String modelOutput,
        boolean fallback,
        long startedAt
    ) {
        try {
            send(emitter, "phase", Map.of("step", "validate", "message", "正在校验完整 HTML 并创建版本"));
            String html = htmlExtractor.extract(modelOutput);
            ProjectDetail project = projectService.completeGeneration(guestId, projectId, prompt, html);
            long durationMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            send(emitter, "completed", Map.of(
                "project", project,
                "fallback", fallback,
                "model", fallback ? "local-fallback" : aiClient.model(),
                "durationMs", durationMs
            ));
            emitter.complete();
        } catch (Exception exception) {
            fail(emitter, exception);
        }
    }

    private void fail(SseEmitter emitter, Throwable error) {
        String message = error.getMessage() == null ? "Generation failed" : error.getMessage();
        sendSafely(emitter, "generation-error", Map.of("message", message));
        emitter.complete();
    }

    private void send(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException exception) {
            throw new IllegalStateException("Client disconnected from generation stream", exception);
        }
    }

    private void sendSafely(SseEmitter emitter, String name, Object data) {
        try {
            send(emitter, name, data);
        } catch (RuntimeException ignored) {
            // The subscriber may still receive upstream completion after the browser disconnects.
        }
    }
}
