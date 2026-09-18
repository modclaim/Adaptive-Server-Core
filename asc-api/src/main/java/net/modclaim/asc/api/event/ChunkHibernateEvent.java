package net.modclaim.asc.api.event;

import net.modclaim.asc.api.lazysim.HibernationTicket;
import org.bukkit.Chunk;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a chunk unloads and enters lazy simulation hibernation.
 */
public class ChunkHibernateEvent extends ASCEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Chunk chunk;
    private final HibernationTicket ticket;

    public ChunkHibernateEvent(@NotNull Chunk chunk, @NotNull HibernationTicket ticket) {
        this.chunk = chunk;
        this.ticket = ticket;
    }

    @NotNull
    public Chunk getChunk() {
        return chunk;
    }

    @NotNull
    public HibernationTicket getTicket() {
        return ticket;
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
