package net.modclaim.asc.api.mobcap;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable snapshot of the server load budget and category allowances.
 */
public final class LoadBudget {
    private final double tps;
    private final double mspt;
    private final double budgetMultiplier;
    private final int onlinePlayers;
    private final Map<MobCategory, Integer> categoryCaps;
    private final long timestamp;

    public LoadBudget(double tps, double mspt, double budgetMultiplier, int onlinePlayers, Map<MobCategory, Integer> categoryCaps) {
        this.tps = tps;
        this.mspt = mspt;
        this.budgetMultiplier = budgetMultiplier;
        this.onlinePlayers = onlinePlayers;
        this.categoryCaps = Collections.unmodifiableMap(categoryCaps);
        this.timestamp = System.currentTimeMillis();
    }

    public double getTps() {
        return tps;
    }

    public double getMspt() {
        return mspt;
    }

    public double getBudgetMultiplier() {
        return budgetMultiplier;
    }

    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    public Map<MobCategory, Integer> getCategoryCaps() {
        return categoryCaps;
    }

    public int getCap(MobCategory category) {
        return categoryCaps.getOrDefault(category, category.getDefaultCap());
    }

    public long getTimestamp() {
        return timestamp;
    }
}
