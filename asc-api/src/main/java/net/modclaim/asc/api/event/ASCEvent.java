package net.modclaim.asc.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Base Bukkit event class for all Adaptive Server Core events.
 */
public abstract class ASCEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public ASCEvent() {
        super(false);
    }

    public ASCEvent(boolean isAsync) {
        super(isAsync);
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
