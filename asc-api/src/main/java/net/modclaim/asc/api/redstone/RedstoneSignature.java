package net.modclaim.asc.api.redstone;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents an identified oscillating redstone or piston loop signature.
 */
public final class RedstoneSignature {
    private final String chunkKey;
    private final Location originLocation;
    private final int updateFrequency;
    private final long signatureHash;
    private final long firstDetectedEpochMs;
    private final long lastDetectedEpochMs;

    public RedstoneSignature(
            @NotNull String chunkKey,
            @NotNull Location originLocation,
            int updateFrequency,
            long signatureHash,
            long firstDetectedEpochMs,
            long lastDetectedEpochMs
    ) {
        this.chunkKey = Objects.requireNonNull(chunkKey, "chunkKey");
        this.originLocation = Objects.requireNonNull(originLocation, "originLocation");
        this.updateFrequency = updateFrequency;
        this.signatureHash = signatureHash;
        this.firstDetectedEpochMs = firstDetectedEpochMs;
        this.lastDetectedEpochMs = lastDetectedEpochMs;
    }

    public String getChunkKey() {
        return chunkKey;
    }

    public Location getOriginLocation() {
        return originLocation;
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }

    public long getSignatureHash() {
        return signatureHash;
    }

    public long getFirstDetectedEpochMs() {
        return firstDetectedEpochMs;
    }

    public long getLastDetectedEpochMs() {
        return lastDetectedEpochMs;
    }
}
