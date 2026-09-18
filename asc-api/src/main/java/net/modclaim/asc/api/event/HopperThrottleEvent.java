package net.modclaim.asc.api.event;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when hopper item transfers in a chunk exceed rate limits and are throttled.
 */
public class HopperThrottleEvent extends ASCEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private final int currentTransfersPerSecond;
    private final int configuredLimit;
    private boolean cancelled;

    public HopperThrottleEvent(@NotNull Location location, int currentTransfersPerSecond, int configuredLimit) {
        this.location = location;
        this.currentTransfersPerSecond = currentTransfersPerSecond;
        this.configuredLimit = configuredLimit;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }

    public int getCurrentTransfersPerSecond() {
        return currentTransfersPerSecond;
    }

    public int getConfiguredLimit() {
        return configuredLimit;
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
