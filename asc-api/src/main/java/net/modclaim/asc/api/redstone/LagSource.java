package net.modclaim.asc.api.redstone;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Encapsulates profiling metrics for a high-cost chunk or coordinate.
 */
public final class LagSource implements Comparable<LagSource> {
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final Location representativeLocation;
    private final int redstoneUpdatesPerSec;
    private final int hopperTransfersPerSec;
    private final int entityCount;
    private final double estimatedMsptImpact;
    private final boolean isThrottled;

    public LagSource(
            @NotNull String worldName,
            int chunkX,
            int chunkZ,
            @NotNull Location representativeLocation,
            int redstoneUpdatesPerSec,
            int hopperTransfersPerSec,
            int entityCount,
            double estimatedMsptImpact,
            boolean isThrottled
    ) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.representativeLocation = Objects.requireNonNull(representativeLocation, "representativeLocation");
        this.redstoneUpdatesPerSec = redstoneUpdatesPerSec;
        this.hopperTransfersPerSec = hopperTransfersPerSec;
        this.entityCount = entityCount;
        this.estimatedMsptImpact = estimatedMsptImpact;
        this.isThrottled = isThrottled;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Location getRepresentativeLocation() {
        return representativeLocation;
    }

    public int getRedstoneUpdatesPerSec() {
        return redstoneUpdatesPerSec;
    }

    public int getHopperTransfersPerSec() {
        return hopperTransfersPerSec;
    }

    public int getEntityCount() {
        return entityCount;
    }

    public double getEstimatedMsptImpact() {
        return estimatedMsptImpact;
    }

    public boolean isThrottled() {
        return isThrottled;
    }

    @Override
    public int compareTo(@NotNull LagSource o) {
        return Double.compare(o.estimatedMsptImpact, this.estimatedMsptImpact);
    }
}
