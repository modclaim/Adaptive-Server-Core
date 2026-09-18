package net.modclaim.asc.api.event;

import net.modclaim.asc.api.chunk.ChunkPriority;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when chunk generation or high-speed loading is queued or throttled.
 */
public class ChunkThrottleEvent extends ASCEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int chunkX;
    private final int chunkZ;
    private ChunkPriority priority;
    private boolean cancelled;

    public ChunkThrottleEvent(@NotNull Player player, int chunkX, int chunkZ, @NotNull ChunkPriority priority) {
        this.player = player;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.priority = priority;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    @NotNull
    public ChunkPriority getPriority() {
        return priority;
    }

    public void setPriority(@NotNull ChunkPriority priority) {
        this.priority = priority;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
