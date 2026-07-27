package dev.buildtrace.generation;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HtmlExtractor {

    public String extract(String modelOutput) {
        if (modelOutput == null || modelOutput.isBlank()) {
            throw new IllegalArgumentException("Model returned an empty response");
        }

        String cleaned = modelOutput
            .replace("```html", "")
            .replace("```HTML", "")
            .replace("```", "")
            .trim();
        String lower = cleaned.toLowerCase(Locale.ROOT);

        int start = lower.indexOf("<!doctype html");
        if (start < 0) {
            start = lower.indexOf("<html");
        }
        int end = lower.lastIndexOf("</html>");
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Model response does not contain a complete HTML document");
        }

        String html = cleaned.substring(start, end + "</html>".length()).trim();
        String htmlLower = html.toLowerCase(Locale.ROOT);
        if (html.length() < 200 || !htmlLower.contains("<body") || !htmlLower.contains("</body>")) {
            throw new IllegalArgumentException("Generated HTML is incomplete");
        }
        return html;
    }
}
