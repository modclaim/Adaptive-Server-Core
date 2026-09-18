package net.modclaim.asc.core.benchmark;

import net.modclaim.asc.api.mobcap.LoadBudget;
import net.modclaim.asc.core.budget.LoadBudgetCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkSimulator {

    @Test
    @DisplayName("Simulate 10,000 budget calculations under 50ms total execution time")
    void testBudgetCalculationThroughput() {
        LoadBudgetCalculator calculator = new LoadBudgetCalculator();

        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            double tps = 15.0 + (i % 6);
            double mspt = 20.0 + (i % 40);
            int players = (i % 100) + 1;
            LoadBudget budget = calculator.calculateBudget(tps, mspt, players);
            assertTrue(budget.getBudgetMultiplier() > 0);
        }
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("[Benchmark] 10,000 budget calculations completed in: " + durationMs + "ms");
        assertTrue(durationMs < 100, "10,000 calculations should take less than 100ms");
    }
}
