package dev.buildtrace.generation;

import dev.buildtrace.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CodexCliGenerationClientTest {

    @TempDir
    Path tempDirectory;

    @Test
    void readsTheFinalResponseProducedByTheCli() throws Exception {
        Path executable = tempDirectory.resolve("fake-codex");
        Files.writeString(executable, """
            #!/bin/sh
            output=""
            while [ "$#" -gt 0 ]; do
              if [ "$1" = "--output-last-message" ]; then
                shift
                output="$1"
              fi
              shift
            done
            cat > "$output" <<'EOF'
            <!DOCTYPE html><html><head><style>body { color: black; }</style></head><body><button>Works</button><script>document.querySelector('button').onclick=()=>{};</script></body></html>
            EOF
            """);
        assertThat(executable.toFile().setExecutable(true)).isTrue();

        AiProperties properties = new AiProperties(
            "codex-cli",
            "https://example.invalid",
            "",
            "unused",
            executable.toString(),
            "",
            "low",
            Duration.ofSeconds(2)
        );

        String result = new CodexCliGenerationClient(properties)
            .stream("generate an application")
            .blockFirst();

        assertThat(result)
            .startsWith("<!DOCTYPE html>")
            .contains("<button>Works</button>");
    }
}
