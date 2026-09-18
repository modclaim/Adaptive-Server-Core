package net.modclaim.asc.api.event;

import net.modclaim.asc.api.lazysim.CatchUpResult;
import org.bukkit.Chunk;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

/**
 * Fired when an unloaded chunk reawakens and completes catch-up simulation.
 */
public class ChunkCatchUpEvent extends ASCEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Chunk chunk;
    private final Duration elapsed;
    private final List<CatchUpResult> results;

    public ChunkCatchUpEvent(@NotNull Chunk chunk, @NotNull Duration elapsed, @NotNull List<CatchUpResult> results) {
        this.chunk = chunk;
        this.elapsed = elapsed;
        this.results = results;
    }

    @NotNull
    public Chunk getChunk() {
        return chunk;
    }

    @NotNull
    public Duration getElapsed() {
        return elapsed;
    }

    @NotNull
    public List<CatchUpResult> getResults() {
        return results;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
