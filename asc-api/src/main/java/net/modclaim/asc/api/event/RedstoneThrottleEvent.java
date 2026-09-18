package net.modclaim.asc.api.event;

import net.modclaim.asc.api.redstone.RedstoneSignature;
import net.modclaim.asc.api.redstone.ThrottleAction;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a redstone mechanism update is throttled.
 */
public class RedstoneThrottleEvent extends ASCEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Block block;
    private final Location location;
    private final RedstoneSignature signature;
    private ThrottleAction action;
    private boolean cancelled;

    public RedstoneThrottleEvent(
            @NotNull Block block,
            @NotNull Location location,
            @NotNull RedstoneSignature signature,
            @NotNull ThrottleAction action
    ) {
        this.block = block;
        this.location = location;
        this.signature = signature;
        this.action = action;
    }

    @NotNull
    public Block getBlock() {
        return block;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }

    @NotNull
    public RedstoneSignature getSignature() {
        return signature;
    }

    @NotNull
    public ThrottleAction getAction() {
        return action;
    }

    public void setAction(@NotNull ThrottleAction action) {
        this.action = action;
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
