package net.modclaim.asc.lazysim.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmeltingCalculatorTest {

    @Test
    @DisplayName("Smelts 8 iron ingots from 1 coal (1600 ticks) in normal furnace (200 ticks/item)")
    void testStandardSmelting() {
        SmeltingCalculator.Result res = SmeltingCalculator.calculate(
                0,    // current cook time
                0,    // current burn time
                200,  // cook total per item
                1600, // fuel value (coal)
                10,   // raw iron count
                1,    // coal count
                0,    // current result count
                64,   // max stack
                1600  // elapsed ticks
        );

        assertEquals(8, res.itemsSmelted, "1 coal should smelt exactly 8 items in 1600 ticks");
        assertEquals(1, res.fuelItemsConsumed, "1 coal consumed");
        assertEquals(0, res.remainingCookTime);
        assertEquals(0, res.remainingBurnTime);
    }

    @Test
    @DisplayName("Zero fuel results in 0 items smelted")
    void testNoFuel() {
        SmeltingCalculator.Result res = SmeltingCalculator.calculate(
                0, 0, 200, 1600, 64, 0, 0, 64, 5000
        );

        assertEquals(0, res.itemsSmelted);
        assertEquals(0, res.fuelItemsConsumed);
    }

    @Test
    @DisplayName("Respects result slot capacity without overflowing stack")
    void testResultSlotCapacity() {
        // Result slot already has 62 items, only 2 items of capacity remain
        SmeltingCalculator.Result res = SmeltingCalculator.calculate(
                0, 0, 200, 1600, 64, 10, 62, 64, 10000
        );

        assertEquals(2, res.itemsSmelted, "Must not exceed stack capacity (max 64)");
        assertTrue(res.fuelItemsConsumed >= 1);
    }
}
