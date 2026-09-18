package net.modclaim.asc.lazysim.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CropGrowthCalculatorTest {

    @Test
    @DisplayName("Advances 3 stages in 720 seconds on hydrated farmland")
    void testHydratedCropGrowth() {
        // 240s per stage. 720s -> 3 stages. Current age 0 -> 3.
        int newAge = CropGrowthCalculator.calculateNewAge(0, 7, true, 15, 720);
        assertEquals(3, newAge);
    }

    @Test
    @DisplayName("Dry soil grows twice as slow")
    void testDrySoilGrowth() {
        // 480s per stage on dry soil. 480s -> 1 stage. Current age 0 -> 1.
        int newAge = CropGrowthCalculator.calculateNewAge(0, 7, false, 15, 480);
        assertEquals(1, newAge);
    }

    @Test
    @DisplayName("Crops do not grow in darkness (light < 8)")
    void testDarknessNoGrowth() {
        int newAge = CropGrowthCalculator.calculateNewAge(2, 7, true, 4, 3600);
        assertEquals(2, newAge, "Age should remain unchanged without light");
    }

    @Test
    @DisplayName("Crops clamp strictly to maximum age")
    void testMaxAgeClamping() {
        int newAge = CropGrowthCalculator.calculateNewAge(5, 7, true, 15, 100000);
        assertEquals(7, newAge, "Age should not exceed maxAge");
    }
}
