package net.modclaim.asc.lazysim.processor;

import net.modclaim.asc.api.lazysim.CatchUpContext;
import net.modclaim.asc.api.lazysim.CatchUpResult;
import net.modclaim.asc.api.lazysim.LazySimulatable;
import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Beehive;
import org.jetbrains.annotations.NotNull;

/**
 * Advances beehive and bee nest honey levels during chunk hibernation.
 */
public final class BeehiveProcessor implements LazySimulatable {

    @Override
    @NotNull
    public String getId() {
        return "beehives";
    }

    @Override
    public int getPriority() {
        return 50;
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

        int beehivesUpdated = 0;
        int honeyToAdd = (int) (elapsedSeconds / 1200); // 1 honey level per MC day

        if (honeyToAdd > 0) {
            for (BlockState state : chunk.getTileEntities()) {
                if (state.getBlockData() instanceof Beehive beehive) {
                    int currentLevel = beehive.getHoneyLevel();
                    int maxLevel = beehive.getMaximumHoneyLevel();
                    if (currentLevel < maxLevel) {
                        beehive.setHoneyLevel(Math.min(maxLevel, currentLevel + honeyToAdd));
                        state.setBlockData(beehive);
                        state.update(true);
                        beehivesUpdated++;
                    }
                }
            }
        }

        long duration = System.nanoTime() - start;
        return new CatchUpResult(getId(), 0, beehivesUpdated, duration, true, "Beehives updated: " + beehivesUpdated);
    }

    @Override
    public void onHibernate(@NotNull Chunk chunk, long epochMs) {
        // No-op
    }
}
