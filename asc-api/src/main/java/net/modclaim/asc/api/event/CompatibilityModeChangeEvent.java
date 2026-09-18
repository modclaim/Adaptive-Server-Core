package net.modclaim.asc.api.event;

import net.modclaim.asc.api.compatibility.CompatibilityMode;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when the active compatibility mode changes.
 */
public class CompatibilityModeChangeEvent extends ASCEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final CompatibilityMode previousMode;
    private final CompatibilityMode newMode;
    private final String reason;

    public CompatibilityModeChangeEvent(
            @NotNull CompatibilityMode previousMode,
            @NotNull CompatibilityMode newMode,
            @NotNull String reason
    ) {
        this.previousMode = previousMode;
        this.newMode = newMode;
        this.reason = reason;
    }

    @NotNull
    public CompatibilityMode getPreviousMode() {
        return previousMode;
    }

    @NotNull
    public CompatibilityMode getNewMode() {
        return newMode;
    }

    @NotNull
    public String getReason() {
        return reason;
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
