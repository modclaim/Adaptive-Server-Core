package net.modclaim.asc.core.redstone;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks and limits hopper item transfer rates per chunk to protect against hopper spam machines.
 */
public final class HopperWatchdog {

    private final int transferLimitPerSecond;
    private final Map<String, AtomicInteger> transferCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastSecondTotals = new ConcurrentHashMap<>();

    public HopperWatchdog(int transferLimitPerSecond) {
        this.transferLimitPerSecond = transferLimitPerSecond;
    }

    public HopperWatchdog() {
        this(50);
    }

    public boolean recordAndCheckTransfer(@NotNull Location location) {
        if (location.getWorld() == null) return true;
        String chunkKey = location.getWorld().getName() + ":" + (location.getBlockX() >> 4) + ":" + (location.getBlockZ() >> 4);

        AtomicInteger counter = transferCounts.computeIfAbsent(chunkKey, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        return current <= transferLimitPerSecond;
    }

    public int getTransfersInChunk(@NotNull String chunkKey) {
        return lastSecondTotals.getOrDefault(chunkKey, 0);
    }

    public void resetPerSecondWindow() {
        for (Map.Entry<String, AtomicInteger> entry : transferCounts.entrySet()) {
            lastSecondTotals.put(entry.getKey(), entry.getValue().getAndSet(0));
        }
        // Prune inactive chunks
        transferCounts.entrySet().removeIf(e -> e.getValue().get() == 0 && lastSecondTotals.getOrDefault(e.getKey(), 0) == 0);
    }
}
