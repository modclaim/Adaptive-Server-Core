package net.modclaim.asc.api.chunk;

/**
 * Priority classification for chunk generation and loading requests.
 */
public enum ChunkPriority {
    IMMEDIATE(1),
    HIGH(2),
    NORMAL(3),
    PREDICTIVE_LOW(4),
    BACKGROUND(5);

    private final int level;

    ChunkPriority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
