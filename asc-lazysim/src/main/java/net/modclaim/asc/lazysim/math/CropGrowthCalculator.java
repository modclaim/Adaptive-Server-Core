package net.modclaim.asc.lazysim.math;

/**
 * Deterministic estimation model for crop growth stages based on elapsed time, hydration, and lighting.
 */
public final class CropGrowthCalculator {

    private static final int BASE_SECONDS_PER_STAGE_HYDRATED = 240; // 4 minutes per stage on hydrated farmland
    private static final int BASE_SECONDS_PER_STAGE_DRY = 480;      // 8 minutes per stage on dry soil

    public static int calculateNewAge(int currentAge, int maxAge, boolean hydrated, int lightLevel, long elapsedSeconds) {
        if (currentAge >= maxAge || elapsedSeconds <= 0) {
            return currentAge;
        }

        // Crops do not grow without sufficient light (vanilla requires light >= 8)
        if (lightLevel < 8) {
            return currentAge;
        }

        int secondsPerStage = hydrated ? BASE_SECONDS_PER_STAGE_HYDRATED : BASE_SECONDS_PER_STAGE_DRY;
        int stagesAdvanced = (int) (elapsedSeconds / secondsPerStage);

        return Math.min(maxAge, currentAge + stagesAdvanced);
    }
}
