package net.modclaim.asc.api.event;

import net.modclaim.asc.api.mobcap.LoadBudget;
import net.modclaim.asc.api.mobcap.MobCategory;
import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when the dynamic mob cap is recalculated for a category in a world.
 */
public class MobCapAdjustEvent extends ASCEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final World world;
    private final MobCategory category;
    private final LoadBudget budget;
    private int newCap;
    private boolean cancelled;

    public MobCapAdjustEvent(@NotNull World world, @NotNull MobCategory category, @NotNull LoadBudget budget, int newCap) {
        this.world = world;
        this.category = category;
        this.budget = budget;
        this.newCap = newCap;
    }

    @NotNull
    public World getWorld() {
        return world;
    }

    @NotNull
    public MobCategory getCategory() {
        return category;
    }

    @NotNull
    public LoadBudget getBudget() {
        return budget;
    }

    public int getNewCap() {
        return newCap;
    }

    public void setNewCap(int newCap) {
        this.newCap = Math.max(0, newCap);
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
