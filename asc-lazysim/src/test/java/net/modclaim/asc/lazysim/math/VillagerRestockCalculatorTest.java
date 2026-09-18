package net.modclaim.asc.lazysim.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillagerRestockCalculatorTest {

    @Test
    @DisplayName("1 Minecraft day (1200s) provides 2 restock cycles")
    void testDailyRestock() {
        // Current uses = 10, max uses = 5.
        // In 1200 seconds (1 MC day), 2 cycles * 5 uses = 10 uses restored -> 0 uses remaining
        int remainingUses = VillagerRestockCalculator.calculateRestockedUses(10, 5, 1200);
        assertEquals(0, remainingUses);
    }

    @Test
    @DisplayName("Zero elapsed time preserves current uses")
    void testZeroTime() {
        int remainingUses = VillagerRestockCalculator.calculateRestockedUses(8, 4, 0);
        assertEquals(8, remainingUses);
    }
}
