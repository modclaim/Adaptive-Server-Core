package net.modclaim.asc.core.redstone;

import net.modclaim.asc.api.event.HopperThrottleEvent;
import net.modclaim.asc.api.event.RedstoneThrottleEvent;
import net.modclaim.asc.api.redstone.LagSource;
import net.modclaim.asc.api.redstone.RedstoneSignature;
import net.modclaim.asc.api.redstone.RedstoneWatchdogService;
import net.modclaim.asc.api.redstone.ThrottleAction;
import net.modclaim.asc.core.scheduler.ServerSchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of RedstoneWatchdogService with signature loop detection and hopper rate throttling.
 */
public final class DefaultRedstoneWatchdogService implements RedstoneWatchdogService, Listener {

    private final Plugin plugin;
    private final ServerSchedulerAdapter scheduler;
    private final SignatureLoopDetector loopDetector;
    private final HopperWatchdog hopperWatchdog;
    private final LagSourceTracker lagSourceTracker;

    private boolean enabled = true;
    private final Set<String> whitelistedChunks = ConcurrentHashMap.newKeySet();

    public DefaultRedstoneWatchdogService(@NotNull Plugin plugin, @NotNull ServerSchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.loopDetector = new SignatureLoopDetector();
        this.hopperWatchdog = new HopperWatchdog(50);
        this.lagSourceTracker = new LagSourceTracker(loopDetector, hopperWatchdog);
    }

    @Override
    public void enable() {
        this.enabled = true;
        // Schedule 1-second rolling reset for hopper metrics
        scheduler.runAsyncTimer(hopperWatchdog::resetPerSecondWindow, 1000L, 1000L);
        // Schedule cleanup of stale loop detector records
        scheduler.runAsyncTimer(loopDetector::cleanupStaleRecords, 5000L, 5000L);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        this.enabled = false;
        whitelistedChunks.clear();
    }

    @Override
    public void reload() {
        // Nothing special to reload
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "RedstoneWatchdogService";
    }

    private static String chunkKey(String world, int cx, int cz) {
        return world + ":" + cx + ":" + cz;
    }

    @Override
    public boolean isWhitelisted(@NotNull Chunk chunk) {
        return whitelistedChunks.contains(chunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
    }

    @Override
    public void addWhitelist(@NotNull String worldName, int chunkX, int chunkZ) {
        whitelistedChunks.add(chunkKey(worldName, chunkX, chunkZ));
    }

    @Override
    public void removeWhitelist(@NotNull String worldName, int chunkX, int chunkZ) {
        whitelistedChunks.remove(chunkKey(worldName, chunkX, chunkZ));
    }

    @Override
    @NotNull
    public ThrottleAction processBlockUpdate(@NotNull Block block, int oldCurrent, int newCurrent) {
        if (!enabled) return ThrottleAction.NONE;
        if (isWhitelisted(block.getChunk())) return ThrottleAction.NONE;

        ThrottleAction action = loopDetector.recordAndCheck(block, oldCurrent, newCurrent);

        if (action == ThrottleAction.THROTTLE_PULSE) {
            String cKey = chunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
            Optional<RedstoneSignature> optSig = loopDetector.getSignatureForChunk(cKey);
            if (optSig.isPresent()) {
                RedstoneThrottleEvent event = new RedstoneThrottleEvent(block, block.getLocation(), optSig.get(), action);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return ThrottleAction.NONE;
                }
                return event.getAction();
            }
        }
        return action;
    }

    @Override
    public boolean canTransferItem(@NotNull Location sourceLocation) {
        if (!enabled) return true;
        if (sourceLocation.getWorld() != null && isWhitelisted(sourceLocation.getChunk())) {
            return true;
        }

        boolean allowed = hopperWatchdog.recordAndCheckTransfer(sourceLocation);
        if (!allowed) {
            HopperThrottleEvent event = new HopperThrottleEvent(sourceLocation, 50, 50);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled(); // if cancelled, allowed to proceed
        }
        return true;
    }

    @Override
    @NotNull
    public List<LagSource> getTopLagSources(int limit) {
        return lagSourceTracker.getTopLagSources(limit);
    }

    @Override
    @NotNull
    public Optional<RedstoneSignature> getSignatureAt(@NotNull Location location) {
        if (location.getWorld() == null) return Optional.empty();
        String cKey = chunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        return loopDetector.getSignatureForChunk(cKey);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (!enabled) return;

        Block block = event.getBlock();
        int oldVal = event.getOldCurrent();
        int newVal = event.getNewCurrent();

        // If oscillating rapidly, process
        ThrottleAction action = processBlockUpdate(block, oldVal, newVal);
        if (action == ThrottleAction.THROTTLE_PULSE) {
            // Keep old value to throttle rapid oscillation
            event.setNewCurrent(oldVal);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!enabled) return;

        Location loc = event.getSource().getLocation();
        if (loc != null && !canTransferItem(loc)) {
            event.setCancelled(true);
        }
    }
}
