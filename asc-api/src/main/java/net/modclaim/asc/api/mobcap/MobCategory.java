package net.modclaim.asc.api.mobcap;

/**
 * Categorization of entities for budget allocation and cap calculation.
 */
public enum MobCategory {
    MONSTER(70, 1.0),
    CREATURE(10, 0.5),
    AMBIENT(15, 0.2),
    WATER_CREATURE(5, 0.4),
    WATER_AMBIENT(20, 0.2),
    UNDERGROUND_WATER_CREATURE(5, 0.4),
    AXOLOTLS(5, 0.3),
    VILLAGER(25, 1.2),
    MISC(10, 0.2);

    private final int defaultCap;
    private final double baseCost;

    MobCategory(int defaultCap, double baseCost) {
        this.defaultCap = defaultCap;
        this.baseCost = baseCost;
    }

    public int getDefaultCap() {
        return defaultCap;
    }

    public double getBaseCost() {
        return baseCost;
    }
}
