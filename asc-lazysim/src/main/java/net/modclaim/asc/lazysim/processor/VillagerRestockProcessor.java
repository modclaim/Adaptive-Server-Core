package net.modclaim.asc.lazysim.processor;

import net.modclaim.asc.api.lazysim.CatchUpContext;
import net.modclaim.asc.api.lazysim.CatchUpResult;
import net.modclaim.asc.api.lazysim.LazySimulatable;
import net.modclaim.asc.lazysim.math.VillagerRestockCalculator;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Restores villager trading stock based on elapsed Minecraft day cycles.
 */
public final class VillagerRestockProcessor implements LazySimulatable {

    @Override
    @NotNull
    public String getId() {
        return "villagers";
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    @NotNull
    public CatchUpResult onCatchUp(@NotNull CatchUpContext context) {
        long start = System.nanoTime();
        Chunk chunk = context.getChunk();
        long elapsedSeconds = context.getElapsed().toSeconds();
        if (elapsedSeconds <= 0) {
            return CatchUpResult.empty(getId());
        }

        int villagersProcessed = 0;

        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Villager villager)) continue;

            List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
            boolean modified = false;

            for (MerchantRecipe recipe : recipes) {
                if (recipe.getUses() > 0) {
                    int newUses = VillagerRestockCalculator.calculateRestockedUses(
                            recipe.getUses(), recipe.getMaxUses(), elapsedSeconds
                    );
                    if (newUses != recipe.getUses()) {
                        recipe.setUses(newUses);
                        modified = true;
                    }
                }
            }

            if (modified) {
                villager.setRecipes(recipes);
                villagersProcessed++;
            }
        }

        long duration = System.nanoTime() - start;
        return new CatchUpResult(getId(), villagersProcessed, 0, duration, true, "Villagers restocked: " + villagersProcessed);
    }

    @Override
    public void onHibernate(@NotNull Chunk chunk, long epochMs) {
        // No-op
    }
}
