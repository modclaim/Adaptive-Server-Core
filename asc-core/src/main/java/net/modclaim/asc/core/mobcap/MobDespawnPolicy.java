package net.modclaim.asc.core.mobcap;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.NotNull;

/**
 * Strict despawn whitelist engine ensuring critical, named, tamed, or player-associated entities are never pruned.
 */
public final class MobDespawnPolicy {

    public static boolean isProtectedFromDespawn(@NotNull Entity entity) {
        // 1. Players, NPCs, or entities with players as passengers / drivers
        if (entity instanceof Player) {
            return true;
        }

        if (entity.isInsideVehicle() && entity.getVehicle() instanceof Player) {
            return true;
        }

        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof Player) {
                return true;
            }
        }

        // 2. Custom Named entities (Name tags, RPG bosses)
        if (entity.getCustomName() != null || entity.isCustomNameVisible()) {
            return true;
        }

        // 3. Persistent flag (Bukkit / Paper persistence marker)
        if (entity.isPersistent()) {
            return true;
        }

        // 4. LivingEntity checks (leashed, equipment)
        if (entity instanceof LivingEntity living) {
            if (living.isLeashed()) {
                return true;
            }

            if (!living.getRemoveWhenFarAway()) {
                return true;
            }
        }

        // 5. Tameable animals (wolves, cats, horses, parrots)
        if (entity instanceof Tameable tameable) {
            if (tameable.isTamed()) {
                return true;
            }
        }

        // 6. MythicMobs or Citizens NPC metadata tags
        if (entity.hasMetadata("NPC") || entity.hasMetadata("MythicMob") || entity.hasMetadata("shopkeeper")) {
            return true;
        }

        return false;
    }
}
