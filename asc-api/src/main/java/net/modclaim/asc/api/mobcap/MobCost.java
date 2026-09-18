package net.modclaim.asc.api.mobcap;

import org.bukkit.entity.EntityType;
import java.util.Objects;

/**
 * Represents the load cost weight of a specific entity type.
 */
public final class MobCost {
    private final EntityType entityType;
    private final MobCategory category;
    private final double costWeight;

    public MobCost(EntityType entityType, MobCategory category, double costWeight) {
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.category = Objects.requireNonNull(category, "category");
        this.costWeight = costWeight;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public MobCategory getCategory() {
        return category;
    }

    public double getCostWeight() {
        return costWeight;
    }
}
