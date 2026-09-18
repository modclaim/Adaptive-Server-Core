package net.modclaim.asc.lazysim.processor;

import net.modclaim.asc.api.lazysim.CatchUpContext;
import net.modclaim.asc.api.lazysim.CatchUpResult;
import net.modclaim.asc.api.lazysim.LazySimulatable;
import org.bukkit.Chunk;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Simulates animal maturation and breeding cooldowns during chunk hibernation.
 */
public final class AnimalBreedingProcessor implements LazySimulatable {

    @Override
    @NotNull
    public String getId() {
        return "animals";
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    @NotNull
    public CatchUpResult onCatchUp(@NotNull CatchUpContext context) {
        long start = System.nanoTime();
        Chunk chunk = context.getChunk();
        long elapsedTicks = context.getElapsed().toSeconds() * 20L;
        if (elapsedTicks <= 0) {
            return CatchUpResult.empty(getId());
        }

        int animalsProcessed = 0;

        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof Breedable breedable)) continue;

            if (!breedable.isAdult()) {
                int currentAge = breedable.getAge(); // negative value counting towards 0
                int newAge = (int) Math.min(0, currentAge + elapsedTicks);
                breedable.setAge(newAge);
                if (newAge >= 0) {
                    breedable.setAdult();
                }
                animalsProcessed++;
            }
        }

        long duration = System.nanoTime() - start;
        return new CatchUpResult(getId(), animalsProcessed, 0, duration, true, "Animals matured: " + animalsProcessed);
    }

    @Override
    public void onHibernate(@NotNull Chunk chunk, long epochMs) {
        // No-op
    }
}
