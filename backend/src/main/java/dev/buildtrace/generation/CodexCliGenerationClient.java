package dev.buildtrace.generation;

import dev.buildtrace.config.AiProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CodexCliGenerationClient {

    private final AiProperties properties;

    public CodexCliGenerationClient(AiProperties properties) {
        this.properties = properties;
    }

    public Flux<String> stream(String instruction) {
        return Flux.defer(() -> Flux.just(run(instruction)));
    }

    private String run(String instruction) {
        Path workDirectory = null;
        try {
            workDirectory = Files.createTempDirectory("buildtrace-codex-");
            Path outputFile = workDirectory.resolve("last-message.html");
            Path logFile = workDirectory.resolve("codex.log");
            ProcessBuilder builder = new ProcessBuilder(command(workDirectory, outputFile));
            builder.directory(workDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(logFile.toFile());

            Process process = builder.start();
            try (var stdin = process.getOutputStream()) {
                stdin.write(instruction.getBytes(StandardCharsets.UTF_8));
            }

            boolean finished = process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
                throw new IllegalStateException("Codex CLI timed out after " + properties.timeout());
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Codex CLI exited with code " + process.exitValue());
            }
            if (!Files.exists(outputFile)) {
                throw new IllegalStateException("Codex CLI did not produce a final response");
            }
            String response = Files.readString(outputFile, StandardCharsets.UTF_8).trim();
            if (response.isEmpty()) {
                throw new IllegalStateException("Codex CLI returned an empty response");
            }
            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Codex CLI execution was interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to run Codex CLI", exception);
        } finally {
            deleteRecursively(workDirectory);
        }
    }

    private List<String> command(Path workDirectory, Path outputFile) {
        List<String> command = new ArrayList<>(List.of(
            properties.codexExecutable(),
            "exec",
            "--skip-git-repo-check",
            "--sandbox",
            "read-only",
            "--color",
            "never",
            "-C",
            workDirectory.toString(),
            "-c",
            "mcp_servers={}",
            "-c",
            "model_reasoning_effort=\"" + properties.codexReasoningEffort() + "\"",
            "--output-last-message",
            outputFile.toString()
        ));
        if (properties.codexModel() != null && !properties.codexModel().isBlank()) {
            command.add("--model");
            command.add(properties.codexModel());
        }
        command.add("-");
        return command;
    }

    private void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Temporary output is best-effort cleanup after the process has finished.
                }
            });
        } catch (IOException ignored) {
            // Temporary output is best-effort cleanup after the process has finished.
        }
    }
}
