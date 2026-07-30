package dev.buildtrace.project;

public enum GenerationRunStatus {
    QUEUED,
    GENERATING,
    VALIDATING,
    REPAIRING,
    SUCCEEDED,
    FAILED,
    CANCELLED;

    public boolean terminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
}
