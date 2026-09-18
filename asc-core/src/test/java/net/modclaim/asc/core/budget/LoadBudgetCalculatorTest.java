package net.modclaim.asc.core.budget;

import net.modclaim.asc.api.mobcap.LoadBudget;
import net.modclaim.asc.api.mobcap.MobCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoadBudgetCalculatorTest {

    private LoadBudgetCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new LoadBudgetCalculator(40.0, 20.0, 1.5, 0.25);
    }

    @Test
    @DisplayName("Healthy server gives bonus mob cap multiplier")
    void testHealthyServerBonus() {
        LoadBudget budget = calculator.calculateBudget(20.0, 25.0, 10);
        assertTrue(budget.getBudgetMultiplier() > 1.0, "Multiplier should be > 1.0 on low load");
        assertTrue(budget.getBudgetMultiplier() <= 1.5, "Multiplier should not exceed maxBonusMultiplier");

        // Monster cap should be boosted above base (70)
        assertTrue(budget.getCap(MobCategory.MONSTER) >= 70);
    }

    @Test
    @DisplayName("Solo exploration gives extra bonus")
    void testSoloPlayerBonus() {
        LoadBudget crowded = calculator.calculateBudget(20.0, 20.0, 50);
        LoadBudget solo = calculator.calculateBudget(20.0, 20.0, 2);

        assertTrue(solo.getBudgetMultiplier() >= crowded.getBudgetMultiplier());
    }

    @Test
    @DisplayName("Stressed server smoothly reduces mob cap down to floor")
    void testStressedServerDecay() {
        LoadBudget budget = calculator.calculateBudget(15.0, 55.0, 50);
        assertTrue(budget.getBudgetMultiplier() < 1.0, "Multiplier should drop below 1.0 under load");
        assertTrue(budget.getBudgetMultiplier() >= 0.25, "Multiplier should not drop below floor");

        // Monster cap should be scaled down
        assertTrue(budget.getCap(MobCategory.MONSTER) < 70);
        assertTrue(budget.getCap(MobCategory.MONSTER) >= 1);
    }

    @Test
    @DisplayName("Zero TPS or extreme values safely clamp without NaN or negative values")
    void testExtremeValues() {
        LoadBudget budget = calculator.calculateBudget(0.0, 200.0, 100);
        assertFalse(Double.isNaN(budget.getBudgetMultiplier()));
        assertEquals(0.25, budget.getBudgetMultiplier(), 0.001);

        for (MobCategory cat : MobCategory.values()) {
            assertTrue(budget.getCap(cat) >= 1, "Caps must remain strictly positive");
        }
    }
}
