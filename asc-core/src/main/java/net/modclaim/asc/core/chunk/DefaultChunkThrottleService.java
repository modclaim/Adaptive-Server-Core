package net.modclaim.asc.core.chunk;

import net.modclaim.asc.api.chunk.ChunkPriority;
import net.modclaim.asc.api.chunk.ChunkThrottleService;
import net.modclaim.asc.api.chunk.PlayerFlightProfile;
import net.modclaim.asc.api.event.ChunkThrottleEvent;
import net.modclaim.asc.core.metrics.ServerMetricsTracker;
import net.modclaim.asc.core.scheduler.ServerSchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ChunkThrottleService coordinating elytra trajectory preloading and dynamic distances.
 */
public final class DefaultChunkThrottleService implements ChunkThrottleService, Listener {

    private final Plugin plugin;
    private final ServerMetricsTracker metricsTracker;
    private final ServerSchedulerAdapter scheduler;
    private final ElytraFlightWatchdog elytraWatchdog;
    private final PredictiveChunkQueue predictiveQueue;
    private final DynamicDistanceManager distanceManager;

    private boolean enabled = true;

    private final Set<Long> inFlightChunks = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastTrackEpoch = new ConcurrentHashMap<>();

    public DefaultChunkThrottleService(
            @NotNull Plugin plugin,
            @NotNull ServerMetricsTracker metricsTracker,
            @NotNull ServerSchedulerAdapter scheduler
    ) {
        this.plugin = plugin;
        this.metricsTracker = metricsTracker;
        this.scheduler = scheduler;
        this.elytraWatchdog = new ElytraFlightWatchdog();
        this.predictiveQueue = new PredictiveChunkQueue();
        this.distanceManager = new DynamicDistanceManager(6, 12, scheduler);
    }

    @Override
    public void enable() {
        this.enabled = true;
        // Schedule dynamic distance updates based on MSPT (every 10 seconds)
        scheduler.runAsyncTimer(() -> {
            if (enabled) {
                distanceManager.updateLoad(metricsTracker.getMspt());
            }
        }, 5000L, 10000L);

        // Periodically clear stale in-flight cache (every 30 seconds)
        scheduler.runAsyncTimer(inFlightChunks::clear, 30000L, 30000L);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        this.enabled = false;
        inFlightChunks.clear();
        lastTrackEpoch.clear();
    }

    @Override
    public void reload() {
        inFlightChunks.clear();
        lastTrackEpoch.clear();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "ChunkThrottleService";
    }

    @Override
    public void trackPlayerMovement(@NotNull Player player) {
        if (!enabled) return;

        // ONLY process gliding players (Elytra). Regular walking/running does not overload chunk generation.
        if (!player.isGliding()) {
            elytraWatchdog.removePlayer(player.getUniqueId());
            return;
        }

        long now = System.currentTimeMillis();
        Long lastTime = lastTrackEpoch.get(player.getUniqueId());
        // Rate-limit: sample at most once every 500ms per player (2 times per second instead of 30+ times)
        if (lastTime != null && (now - lastTime) < 500L) {
            return;
        }
        lastTrackEpoch.put(player.getUniqueId(), now);

        elytraWatchdog.updatePlayerFlight(player);

        Optional<PlayerFlightProfile> optProfile = elytraWatchdog.getProfile(player.getUniqueId());
        if (optProfile.isPresent()) {
            PlayerFlightProfile profile = optProfile.get();

            // Only pre-load ahead if server is performing very smoothly (MSPT < 30ms)
            if (metricsTracker.getMspt() < 30.0 && profile.getHorizontalSpeed() > 1.2) {
                List<PredictiveChunkQueue.PredictedChunk> candidates = predictiveQueue.predictCandidateChunks(player, profile);
                World world = player.getWorld();

                for (PredictiveChunkQueue.PredictedChunk candidate : candidates) {
                    long chunkKey = (((long) candidate.chunkX) << 32) | (candidate.chunkZ & 0xFFFFFFFFL);
                    if (!world.isChunkLoaded(candidate.chunkX, candidate.chunkZ) && inFlightChunks.add(chunkKey)) {
                        scheduler.runAtChunk(world, candidate.chunkX, candidate.chunkZ, () -> {
                            world.getChunkAtAsync(candidate.chunkX, candidate.chunkZ).thenAccept(chunk -> {
                                inFlightChunks.remove(chunkKey);
                            }).exceptionally(ex -> {
                                inFlightChunks.remove(chunkKey);
                                return null;
                            });
                        });
                    }
                }
            }
        }
    }

    @Override
    @NotNull
    public Optional<PlayerFlightProfile> getFlightProfile(@NotNull UUID playerUuid) {
        return elytraWatchdog.getProfile(playerUuid);
    }

    @Override
    public boolean requestChunkGeneration(@NotNull Player player, int chunkX, int chunkZ, @NotNull ChunkPriority priority) {
        if (!enabled) return true;

        if (metricsTracker.getMspt() > 48.0 && priority.getLevel() > ChunkPriority.HIGH.getLevel()) {
            ChunkThrottleEvent event = new ChunkThrottleEvent(player, chunkX, chunkZ, priority);
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        }
        return true;
    }

    @Override
    public void adaptPlayerDistances(@NotNull Player player, int targetViewDistance, int targetSimulationDistance) {
        distanceManager.adaptPlayer(player, targetViewDistance, targetSimulationDistance);
    }

    @Override
    public boolean isPerPlayerDistanceSupported() {
        return distanceManager.isPerPlayerSupported();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled) return;
        if (!event.getPlayer().isGliding()) return;
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            trackPlayerMovement(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        elytraWatchdog.removePlayer(event.getPlayer().getUniqueId());
    }
}
