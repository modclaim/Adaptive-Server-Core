package net.modclaim.asc.api.mobcap;

import net.modclaim.asc.api.ASCService;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Service managing real-time dynamic mob caps, spawn budgets, and despawn policies.
 */
public interface MobCapService extends ASCService {

    /**
     * Gets the latest calculated load budget.
     *
     * @return current load budget
     */
    @NotNull
    LoadBudget getCurrentBudget();

    /**
     * Calculates the dynamic mob cap for a specific category in a world.
     *
     * @param world target world
     * @param category mob category
     * @return adjusted cap limit
     */
    int getEffectiveCap(@NotNull World world, @NotNull MobCategory category);

    /**
     * Checks whether an entity of the given type is permitted to spawn under current budget.
     *
     * @param world world where spawn is attempted
     * @param type entity type
     * @param regionId optional region identifier or null
     * @return true if spawn is within budget
     */
    boolean canSpawn(@NotNull World world, @NotNull EntityType type, @Nullable String regionId);

    /**
     * Registers or overrides the cost weight for an entity type.
     *
     * @param cost mob cost entry
     */
    void registerMobCost(@NotNull MobCost cost);

    /**
     * Checks if an entity is protected from automatic despawning.
     *
     * @param entity candidate entity
     * @return true if protected (named, tamed, leashed, persistent, or rider)
     */
    boolean isProtectedFromDespawn(@NotNull Entity entity);

    /**
     * Toggles the live action bar / bossbar HUD for an administrator player.
     *
     * @param player player to toggle monitor for
     * @return true if monitoring is now active
     */
    boolean toggleMonitor(@NotNull Player player);

    /**
     * Checks whether a player is actively monitoring mob caps.
     *
     * @param player player to check
     * @return true if monitoring
     */
    boolean isMonitoring(@NotNull Player player);
}
