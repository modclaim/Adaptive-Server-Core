package net.modclaim.asc.lazysim.math;

/**
 * Pure mathematical simulation model for elapsed furnace smelting and fuel consumption.
 */
public final class SmeltingCalculator {

    public static final class Result {
        public final int itemsSmelted;
        public final int fuelItemsConsumed;
        public final int remainingCookTime;
        public final int remainingBurnTime;

        public Result(int itemsSmelted, int fuelItemsConsumed, int remainingCookTime, int remainingBurnTime) {
            this.itemsSmelted = itemsSmelted;
            this.fuelItemsConsumed = fuelItemsConsumed;
            this.remainingCookTime = remainingCookTime;
            this.remainingBurnTime = remainingBurnTime;
        }
    }

    /**
     * Calculates items cooked and fuel burned over an elapsed number of ticks.
     */
    public static Result calculate(
            int currentCookTime,
            int currentBurnTime,
            int cookTimeTotal,
            int fuelValuePerItem,
            int sourceCount,
            int fuelCount,
            int currentResultCount,
            int maxResultStack,
            long elapsedTicks
    ) {
        if (sourceCount <= 0 || elapsedTicks <= 0 || cookTimeTotal <= 0) {
            return new Result(0, 0, currentCookTime, currentBurnTime);
        }

        int availableResultSpace = maxResultStack - currentResultCount;
        if (availableResultSpace <= 0) {
            return new Result(0, 0, currentCookTime, currentBurnTime);
        }

        long ticksLeft = elapsedTicks;
        int burnTime = currentBurnTime;
        int cookTime = currentCookTime;
        int sourcesRemaining = sourceCount;
        int fuelsRemaining = fuelCount;
        int smelted = 0;
        int fuelsUsed = 0;

        while (ticksLeft > 0 && sourcesRemaining > 0 && smelted < availableResultSpace) {
            if (burnTime <= 0) {
                if (fuelsRemaining > 0 && fuelValuePerItem > 0) {
                    fuelsRemaining--;
                    fuelsUsed++;
                    burnTime += fuelValuePerItem;
                } else {
                    // Out of fuel
                    break;
                }
            }

            int ticksToSmeltOne = cookTimeTotal - cookTime;
            long stepTicks = Math.min(ticksLeft, Math.min(burnTime, ticksToSmeltOne));

            cookTime += (int) stepTicks;
            burnTime -= (int) stepTicks;
            ticksLeft -= stepTicks;

            if (cookTime >= cookTimeTotal) {
                cookTime = 0;
                sourcesRemaining--;
                smelted++;
            }
        }

        return new Result(smelted, fuelsUsed, cookTime, burnTime);
    }
}
