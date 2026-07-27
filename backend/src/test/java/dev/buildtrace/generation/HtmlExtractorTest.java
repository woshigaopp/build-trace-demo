package dev.buildtrace.generation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtmlExtractorTest {

    private final HtmlExtractor extractor = new HtmlExtractor();

    @Test
    void extractsHtmlFromMarkdownFenceAndExplanation() {
        String result = extractor.extract("""
            Here is the application:
            ```html
            <!DOCTYPE html><html><head><title>Demo</title><style>body { font-family: sans-serif; }</style></head><body>
            <main><h1>Generated application</h1><p>This is enough content to represent a complete generated application with useful interactive controls.</p>
            <button type="button" onclick="this.textContent='Done'">Run action</button></main>
            </body></html>
            ```
            """);

        assertThat(result).startsWith("<!DOCTYPE html>");
        assertThat(result).endsWith("</html>");
        assertThat(result).doesNotContain("```");
    }

    @Test
    void rejectsIncompleteOutput() {
        assertThatThrownBy(() -> extractor.extract("<html><body>unfinished"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("complete HTML");
    }
}
