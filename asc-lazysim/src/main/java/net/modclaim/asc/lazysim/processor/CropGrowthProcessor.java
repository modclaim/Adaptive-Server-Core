package net.modclaim.asc.lazysim.processor;

import net.modclaim.asc.api.lazysim.CatchUpContext;
import net.modclaim.asc.api.lazysim.CatchUpResult;
import net.modclaim.asc.api.lazysim.LazySimulatable;
import net.modclaim.asc.lazysim.math.CropGrowthCalculator;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Farmland;
import org.jetbrains.annotations.NotNull;

/**
 * Simulates crop growth progression for farmlands in hibernated chunks.
 */
public final class CropGrowthProcessor implements LazySimulatable {

    @Override
    @NotNull
    public String getId() {
        return "crops";
    }

    @Override
    public int getPriority() {
        return 20;
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

        int blocksUpdated = 0;
        int minY = chunk.getWorld().getMinHeight();
        int maxY = Math.min(minY + 256, chunk.getWorld().getMaxHeight());

        // Fast sample pass: scan for crops in loaded chunk
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                int highest = chunk.getWorld().getHighestBlockYAt((chunk.getX() << 4) + x, (chunk.getZ() << 4) + z);
                for (int y = Math.max(minY, highest - 5); y <= highest + 1; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    BlockData data = block.getBlockData();
                    if (data instanceof Ageable ageable) {
                        int maxAge = ageable.getMaximumAge();
                        int currentAge = ageable.getAge();
                        if (currentAge < maxAge) {
                            Block soil = block.getRelative(0, -1, 0);
                            boolean hydrated = false;
                            if (soil.getBlockData() instanceof Farmland farmland) {
                                hydrated = farmland.getMoisture() > 0;
                            }

                            int newAge = CropGrowthCalculator.calculateNewAge(
                                    currentAge, maxAge, hydrated, block.getLightLevel(), elapsedSeconds
                            );
                            if (newAge != currentAge) {
                                ageable.setAge(newAge);
                                block.setBlockData(ageable, false);
                                blocksUpdated++;
                            }
                        }
                    }
                }
            }
        }

        long duration = System.nanoTime() - start;
        return new CatchUpResult(getId(), 0, blocksUpdated, duration, true, "Crops advanced: " + blocksUpdated);
    }

    @Override
    public void onHibernate(@NotNull Chunk chunk, long epochMs) {
        // No-op
    }
}
