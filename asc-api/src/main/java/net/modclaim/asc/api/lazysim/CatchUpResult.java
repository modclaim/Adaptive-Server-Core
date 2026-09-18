package net.modclaim.asc.api.lazysim;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Outcome metrics of a catch-up simulation pass.
 */
public final class CatchUpResult {
    private final String processorId;
    private final int entitiesProcessed;
    private final int blocksUpdated;
    private final long executionTimeNanos;
    private final boolean successful;
    private final String details;

    public CatchUpResult(
            @NotNull String processorId,
            int entitiesProcessed,
            int blocksUpdated,
            long executionTimeNanos,
            boolean successful,
            @NotNull String details
    ) {
        this.processorId = Objects.requireNonNull(processorId, "processorId");
        this.entitiesProcessed = entitiesProcessed;
        this.blocksUpdated = blocksUpdated;
        this.executionTimeNanos = executionTimeNanos;
        this.successful = successful;
        this.details = Objects.requireNonNull(details, "details");
    }

    public static CatchUpResult empty(@NotNull String processorId) {
        return new CatchUpResult(processorId, 0, 0, 0L, true, "No operations needed");
    }

    public String getProcessorId() {
        return processorId;
    }

    public int getEntitiesProcessed() {
        return entitiesProcessed;
    }

    public int getBlocksUpdated() {
        return blocksUpdated;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getDetails() {
        return details;
    }
}
