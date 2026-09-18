package net.modclaim.asc.core.redstone;

import net.modclaim.asc.api.redstone.LagSource;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects and aggregates real-time metrics to rank the heaviest chunks by estimated MSPT impact.
 */
public final class LagSourceTracker {

    private final SignatureLoopDetector loopDetector;
    private final HopperWatchdog hopperWatchdog;

    public LagSourceTracker(@NotNull SignatureLoopDetector loopDetector, @NotNull HopperWatchdog hopperWatchdog) {
        this.loopDetector = loopDetector;
        this.hopperWatchdog = hopperWatchdog;
    }

    @NotNull
    public List<LagSource> getTopLagSources(int limit) {
        List<LagSource> sources = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                String chunkKey = world.getName() + ":" + chunk.getX() + ":" + chunk.getZ();

                int redstone = loopDetector.getRecentUpdatesInChunk(chunkKey);
                int hoppers = hopperWatchdog.getTransfersInChunk(chunkKey);
                int entities = chunk.getEntities().length;

                // Skip chunks with negligible activity
                if (redstone == 0 && hoppers == 0 && entities < 30) {
                    continue;
                }

                // Estimated tick cost impact in milliseconds
                double estimatedMspt = (redstone * 0.04) + (hoppers * 0.06) + (entities * 0.02);
                boolean throttled = loopDetector.getSignatureForChunk(chunkKey).isPresent();

                Location representativeLoc = new Location(world, (chunk.getX() << 4) + 8, 64, (chunk.getZ() << 4) + 8);

                sources.add(new LagSource(
                        world.getName(),
                        chunk.getX(),
                        chunk.getZ(),
                        representativeLoc,
                        redstone,
                        hoppers,
                        entities,
                        estimatedMspt,
                        throttled
                ));
            }
        }

        Collections.sort(sources);
        if (sources.size() > limit) {
            return sources.subList(0, limit);
        }
        return sources;
    }
}
