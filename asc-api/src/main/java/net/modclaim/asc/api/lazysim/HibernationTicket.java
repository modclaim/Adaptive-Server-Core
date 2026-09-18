package net.modclaim.asc.api.lazysim;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Metadata ticket representing a hibernating or reawakened chunk.
 */
public final class HibernationTicket {
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final long hibernateEpochMs;

    public HibernationTicket(@NotNull String worldName, int chunkX, int chunkZ, long hibernateEpochMs) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.hibernateEpochMs = hibernateEpochMs;
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

    public long getHibernateEpochMs() {
        return hibernateEpochMs;
    }
}
