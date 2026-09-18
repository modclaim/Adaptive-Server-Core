package net.modclaim.asc.lazysim.math;

/**
 * Calculates villager trade restocking based on elapsed Minecraft day cycles.
 */
public final class VillagerRestockCalculator {

    private static final int SECONDS_PER_MC_DAY = 1200; // 20 real-time minutes = 1 MC day

    public static int calculateRestockedUses(int currentUses, int maxUses, long elapsedSeconds) {
        if (currentUses <= 0) {
            return 0;
        }
        if (elapsedSeconds <= 0) {
            return currentUses;
        }

        // In vanilla, villagers restock up to twice a day (every 600s)
        long restockCycles = (elapsedSeconds / (SECONDS_PER_MC_DAY / 2));
        if (restockCycles <= 0) {
            return currentUses;
        }

        // Each restock cycle restores trades
        int usesToReduce = (int) Math.min(currentUses, restockCycles * maxUses);
        return Math.max(0, currentUses - usesToReduce);
    }
}
