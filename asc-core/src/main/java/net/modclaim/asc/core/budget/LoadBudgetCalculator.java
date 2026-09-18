package net.modclaim.asc.core.budget;

import net.modclaim.asc.api.mobcap.LoadBudget;
import net.modclaim.asc.api.mobcap.MobCategory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Calculates adaptive load budgets using smooth mathematical curves based on TPS, MSPT, and player density.
 */
public final class LoadBudgetCalculator {

    private final double targetMspt;
    private final double targetTps;
    private final double maxBonusMultiplier;
    private final double minFloorMultiplier;

    public LoadBudgetCalculator(double targetMspt, double targetTps, double maxBonusMultiplier, double minFloorMultiplier) {
        this.targetMspt = targetMspt;
        this.targetTps = targetTps;
        this.maxBonusMultiplier = maxBonusMultiplier;
        this.minFloorMultiplier = minFloorMultiplier;
    }

    public LoadBudgetCalculator() {
        this(40.0, 20.0, 1.5, 0.20);
    }

    /**
     * Computes the effective load budget snapshot.
     *
     * @param currentTps current ticks per second
     * @param currentMspt current tick duration in milliseconds
     * @param onlinePlayers total players online
     * @return calculated LoadBudget
     */
    public LoadBudget calculateBudget(double currentTps, double currentMspt, int onlinePlayers) {
        double multiplier;

        if (currentMspt <= 35.0 && currentTps >= 19.8) {
            // Server has ample headroom - boost mob cap so RPG world feels alive!
            double headroomFactor = Math.max(0.0, (35.0 - currentMspt) / 35.0);
            multiplier = 1.0 + (maxBonusMultiplier - 1.0) * headroomFactor;

            // Give a slight bonus if low player count (solo exploration)
            if (onlinePlayers <= 5) {
                multiplier = Math.min(maxBonusMultiplier, multiplier * 1.15);
            }
        } else {
            // Server under stress - smoothly decay budget (avoiding jarring step cliffs)
            double msptRatio = targetMspt / Math.max(targetMspt, currentMspt);
            double msptFactor = Math.pow(msptRatio, 1.8);

            double tpsRatio = Math.max(0.0, currentTps - 10.0) / (targetTps - 10.0);
            double tpsFactor = Math.min(1.0, Math.max(0.0, tpsRatio));

            multiplier = msptFactor * tpsFactor;
            multiplier = Math.max(minFloorMultiplier, Math.min(1.0, multiplier));
        }

        Map<MobCategory, Integer> caps = new EnumMap<>(MobCategory.class);
        for (MobCategory cat : MobCategory.values()) {
            int adjusted = (int) Math.round(cat.getDefaultCap() * multiplier);
            caps.put(cat, Math.max(1, adjusted));
        }

        return new LoadBudget(currentTps, currentMspt, multiplier, onlinePlayers, caps);
    }
}
