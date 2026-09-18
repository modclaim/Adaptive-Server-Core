package net.modclaim.asc.api.lazysim;

import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;

/**
 * Context payload passed to LazySimulatable processors upon chunk reawakening.
 */
public final class CatchUpContext {
    private final Chunk chunk;
    private final Duration elapsed;
    private final long lastSimulatedEpochMs;
    private final long currentEpochMs;
    private final boolean degradedMode;

    public CatchUpContext(
            @NotNull Chunk chunk,
            @NotNull Duration elapsed,
            long lastSimulatedEpochMs,
            long currentEpochMs,
            boolean degradedMode
    ) {
        this.chunk = Objects.requireNonNull(chunk, "chunk");
        this.elapsed = Objects.requireNonNull(elapsed, "elapsed");
        this.lastSimulatedEpochMs = lastSimulatedEpochMs;
        this.currentEpochMs = currentEpochMs;
        this.degradedMode = degradedMode;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public Duration getElapsed() {
        return elapsed;
    }

    public long getLastSimulatedEpochMs() {
        return lastSimulatedEpochMs;
    }

    public long getCurrentEpochMs() {
        return currentEpochMs;
    }

    public boolean isDegradedMode() {
        return degradedMode;
    }
}
