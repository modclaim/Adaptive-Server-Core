package net.modclaim.asc.core.metrics;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks rolling TPS, MSPT, and entity statistics with nanosecond resolution.
 */
public final class ServerMetricsTracker {

    private final Plugin plugin;
    private final Deque<Long> tickDurationsNanos = new ArrayDeque<>(600); // last 30 seconds at 20tps
    private final AtomicLong lastTickTimeNanos = new AtomicLong(System.nanoTime());

    private volatile double currentTps = 20.0;
    private volatile double currentMspt = 25.0;
    private volatile int totalEntities = 0;
    private volatile int totalLoadedChunks = 0;

    private Method paperGetTpsMethod;
    private Method paperGetAverageTickTimeMethod;

    public ServerMetricsTracker(@NotNull Plugin plugin) {
        this.plugin = plugin;
        initPaperMetricsReflections();
    }

    private void initPaperMetricsReflections() {
        try {
            this.paperGetTpsMethod = Bukkit.class.getMethod("getTPS");
        } catch (NoSuchMethodException ignored) {}

        try {
            this.paperGetAverageTickTimeMethod = Bukkit.class.getMethod("getAverageTickTime");
        } catch (NoSuchMethodException ignored) {}
    }

    /**
     * Called by the tick timer or tick event on the main/tick thread.
     */
    public void recordTick() {
        long now = System.nanoTime();
        long prev = lastTickTimeNanos.getAndSet(now);
        long duration = now - prev;

        synchronized (tickDurationsNanos) {
            if (tickDurationsNanos.size() >= 600) {
                tickDurationsNanos.pollFirst();
            }
            tickDurationsNanos.addLast(duration);

            long sum = 0;
            for (long d : tickDurationsNanos) {
                sum += d;
            }
            int count = tickDurationsNanos.size();
            double avgNanos = count > 0 ? (double) sum / count : 50_000_000.0;
            this.currentMspt = avgNanos / 1_000_000.0;

            if (avgNanos > 0) {
                double rawTps = 1_000_000_000.0 / avgNanos;
                this.currentTps = Math.min(20.0, Math.max(0.0, rawTps));
            }
        }

        // Try reading native Paper metrics if available
        if (paperGetTpsMethod != null) {
            try {
                double[] tpsArray = (double[]) paperGetTpsMethod.invoke(null);
                if (tpsArray != null && tpsArray.length > 0) {
                    this.currentTps = Math.min(20.0, tpsArray[0]);
                }
            } catch (Exception ignored) {}
        }

        if (paperGetAverageTickTimeMethod != null) {
            try {
                Number msptVal = (Number) paperGetAverageTickTimeMethod.invoke(null);
                if (msptVal != null) {
                    this.currentMspt = msptVal.doubleValue();
                }
            } catch (Exception ignored) {}
        }

        // Update entity and chunk counts periodically
        if (Bukkit.getCurrentTick() % 100 == 0) {
            updateWorldStats();
        }
    }

    public void updateWorldStats() {
        int entities = 0;
        int chunks = 0;
        for (World world : Bukkit.getWorlds()) {
            entities += world.getEntityCount();
            chunks += world.getLoadedChunks().length;
        }
        this.totalEntities = entities;
        this.totalLoadedChunks = chunks;
    }

    public double getTps() {
        return currentTps;
    }

    public double getMspt() {
        return currentMspt;
    }

    public int getTotalEntities() {
        return totalEntities;
    }

    public int getTotalLoadedChunks() {
        return totalLoadedChunks;
    }
}
