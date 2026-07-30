package dev.buildtrace.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Component
public class ProjectFiles {

    private static final int MAX_FILES = 40;
    private static final int MAX_FILE_LENGTH = 120_000;
    private static final int MAX_TOTAL_LENGTH = 600_000;
    private static final Set<String> REQUIRED_FILES = Set.of(
        "/package.json", "/index.html", "/index.jsx", "/App.jsx", "/styles.css", "/vite.config.js");

    private final ObjectMapper objectMapper;

    public ProjectFiles(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, String> candidate(
        Map<String, String> current,
        GenerationResult result
    ) {
        Map<String, String> candidate = new LinkedHashMap<>(current.isEmpty() ? starter() : current);
        for (GenerationResult.FileOperation operation : result.operations()) {
            if (operation == null) {
                throw new IllegalArgumentException("File operation cannot be null");
            }
            String path = normalizePath(operation.path());
            String type = operation.type() == null ? "" : operation.type().trim().toLowerCase();
            switch (type) {
                case "write" -> {
                    if (operation.content() == null) {
                        throw new IllegalArgumentException("Write operation requires content: " + path);
                    }
                    candidate.put(path, operation.content());
                }
                case "delete" -> candidate.remove(path);
                default -> throw new IllegalArgumentException("Unsupported file operation: " + operation.type());
            }
        }
        Map<String, String> ordered = new TreeMap<>(candidate);
        validate(ordered);
        return ordered;
    }

    public void validate(Map<String, String> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Project must contain files");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException("Project contains more than " + MAX_FILES + " files");
        }
        int totalLength = 0;
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String path = normalizePath(entry.getKey());
            if (!path.equals(entry.getKey())) {
                throw new IllegalArgumentException("File path is not normalized: " + entry.getKey());
            }
            if (entry.getValue() == null || entry.getValue().length() > MAX_FILE_LENGTH) {
                throw new IllegalArgumentException("File is empty or too large: " + path);
            }
            totalLength += entry.getValue().length();
        }
        if (totalLength > MAX_TOTAL_LENGTH) {
            throw new IllegalArgumentException("Project source is too large");
        }
        if (!files.keySet().containsAll(REQUIRED_FILES)) {
            throw new IllegalArgumentException("Project is missing required React scaffold files");
        }
        try {
            objectMapper.readTree(files.get("/package.json"));
        } catch (Exception exception) {
            throw new IllegalArgumentException("package.json is invalid JSON", exception);
        }
        if (!files.get("/index.jsx").contains("createRoot")) {
            throw new IllegalArgumentException("index.jsx must mount the React application");
        }
        if (!files.get("/App.jsx").contains("export default")) {
            throw new IllegalArgumentException("App.jsx must export the application component");
        }
    }

    public Map<String, String> starter() {
        return Map.of(
            "/package.json", """
                {"scripts":{"dev":"vite","build":"vite build","preview":"vite preview"},"dependencies":{"lucide-react":"^0.468.0","react":"^18.2.0","react-dom":"^18.2.0"},"devDependencies":{"@vitejs/plugin-react":"3.1.0","vite":"4.1.4","esbuild-wasm":"0.17.12"}}
                """.trim(),
            "/index.html", """
                <!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/><title>BuildTrace App</title></head><body><div id="root"></div><script type="module" src="/index.jsx"></script></body></html>
                """.trim(),
            "/index.jsx", """
                import React from 'react';
                import { createRoot } from 'react-dom/client';
                import App from './App';
                import './styles.css';
                createRoot(document.getElementById('root')).render(<App />);
                """.trim(),
            "/App.jsx", """
                import React from 'react';
                export default function App() {
                  return <main><h1>Starting your app...</h1></main>;
                }
                """.trim(),
            "/styles.css", """
                * { box-sizing: border-box; }
                body { margin: 0; font-family: Inter, system-ui, sans-serif; }
                button, input, textarea { font: inherit; }
                """.trim(),
            "/vite.config.js", """
                import { defineConfig } from 'vite';
                import react from '@vitejs/plugin-react';
                export default defineConfig({ plugins: [react()] });
                """.trim()
        );
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("File path is required");
        }
        String path = rawPath.trim().replace('\\', '/');
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.contains("..") || path.contains("//") || path.length() > 180) {
            throw new IllegalArgumentException("Unsafe file path: " + rawPath);
        }
        return path;
    }
}
