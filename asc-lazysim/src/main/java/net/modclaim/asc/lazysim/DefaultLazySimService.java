package net.modclaim.asc.lazysim;

import net.modclaim.asc.api.event.ChunkCatchUpEvent;
import net.modclaim.asc.api.event.ChunkHibernateEvent;
import net.modclaim.asc.api.lazysim.CatchUpContext;
import net.modclaim.asc.api.lazysim.CatchUpResult;
import net.modclaim.asc.api.lazysim.HibernationTicket;
import net.modclaim.asc.api.lazysim.LazySimService;
import net.modclaim.asc.api.lazysim.LazySimulatable;
import net.modclaim.asc.core.persistence.DatabaseManager;
import net.modclaim.asc.core.scheduler.ServerSchedulerAdapter;
import net.modclaim.asc.lazysim.processor.*;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Central implementation of the Chunk Hibernation & Lazy Simulation engine.
 */
public final class DefaultLazySimService implements LazySimService, Listener {

    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final ServerSchedulerAdapter scheduler;

    private boolean enabled = true;
    private final Map<String, LazySimulatable> processors = new ConcurrentHashMap<>();
    private final Set<LazySimulatable> sortedProcessors = new ConcurrentSkipListSet<>(
            Comparator.comparingInt(LazySimulatable::getPriority).thenComparing(LazySimulatable::getId)
    );

    private static final long MIN_CATCHUP_THRESHOLD_MS = 5000L;          // 5 seconds
    private static final long MAX_CATCHUP_THRESHOLD_MS = 7L * 24 * 3600 * 1000; // 7 days

    public DefaultLazySimService(
            @NotNull Plugin plugin,
            @NotNull DatabaseManager databaseManager,
            @NotNull ServerSchedulerAdapter scheduler
    ) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;

        registerBuiltinProcessors();
    }

    private void registerBuiltinProcessors() {
        registerProcessor(new FurnaceSmeltProcessor());
        registerProcessor(new CropGrowthProcessor());
        registerProcessor(new VillagerRestockProcessor());
        registerProcessor(new AnimalBreedingProcessor());
        registerProcessor(new BeehiveProcessor());
    }

    @Override
    public void enable() {
        this.enabled = true;
        // Periodic pruning of records older than 14 days
        scheduler.runAsyncTimer(this::purgeStaleRecords, 60000L, 3600000L);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
        // Reload processors if needed
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "LazySimService";
    }

    @Override
    public void registerProcessor(@NotNull LazySimulatable processor) {
        processors.put(processor.getId(), processor);
        sortedProcessors.add(processor);
        plugin.getLogger().info("Registered LazySim processor: " + processor.getId() + " (priority=" + processor.getPriority() + ")");
    }

    @Override
    public void unregisterProcessor(@NotNull String processorId) {
        LazySimulatable removed = processors.remove(processorId);
        if (removed != null) {
            sortedProcessors.remove(removed);
        }
    }

    @Override
    @NotNull
    public Collection<LazySimulatable> getProcessors() {
        return Collections.unmodifiableCollection(sortedProcessors);
    }

    @Override
    public void handleChunkUnload(@NotNull Chunk chunk) {
        if (!enabled) return;

        World world = chunk.getWorld();
        long now = System.currentTimeMillis();
        databaseManager.recordChunkHibernation(world.getName(), chunk.getX(), chunk.getZ(), now);

        HibernationTicket ticket = new HibernationTicket(world.getName(), chunk.getX(), chunk.getZ(), now);
        Bukkit.getPluginManager().callEvent(new ChunkHibernateEvent(chunk, ticket));

        for (LazySimulatable processor : sortedProcessors) {
            try {
                processor.onHibernate(chunk, now);
            } catch (Throwable t) {
                plugin.getLogger().warning("Error in onHibernate for processor " + processor.getId() + ": " + t.getMessage());
            }
        }
    }

    @Override
    public void handleChunkLoad(@NotNull Chunk chunk) {
        if (!enabled) return;

        World world = chunk.getWorld();
        Optional<HibernationTicket> optTicket = databaseManager.getChunkHibernation(world.getName(), chunk.getX(), chunk.getZ());
        if (optTicket.isEmpty()) {
            return;
        }

        HibernationTicket ticket = optTicket.get();
        long now = System.currentTimeMillis();
        long elapsedMs = now - ticket.getHibernateEpochMs();

        databaseManager.removeChunkHibernation(world.getName(), chunk.getX(), chunk.getZ());

        // Skip rapid unloads/reloads (less than 5s)
        if (elapsedMs < MIN_CATCHUP_THRESHOLD_MS) {
            return;
        }

        // Clamp to max 7 days to prevent integer overflow or runaway math
        elapsedMs = Math.min(elapsedMs, MAX_CATCHUP_THRESHOLD_MS);
        Duration elapsed = Duration.ofMillis(elapsedMs);

        CatchUpContext context = new CatchUpContext(chunk, elapsed, ticket.getHibernateEpochMs(), now, false);
        List<CatchUpResult> results = new ArrayList<>();

        for (LazySimulatable processor : sortedProcessors) {
            try {
                CatchUpResult res = processor.onCatchUp(context);
                results.add(res);
            } catch (Throwable t) {
                plugin.getLogger().warning("Error during catch-up in processor " + processor.getId() + ": " + t.getMessage());
            }
        }

        Bukkit.getPluginManager().callEvent(new ChunkCatchUpEvent(chunk, elapsed, results));
    }

    @Override
    @NotNull
    public Optional<HibernationTicket> getTicket(@NotNull String worldName, int chunkX, int chunkZ) {
        return databaseManager.getChunkHibernation(worldName, chunkX, chunkZ);
    }

    @Override
    public int getTrackedHibernatingCount() {
        return databaseManager.getTrackedHibernatingCount();
    }

    @Override
    public int purgeStaleRecords() {
        long fourteenDaysAgo = System.currentTimeMillis() - (14L * 24 * 3600 * 1000);
        return databaseManager.purgeOldHibernationRecords(fourteenDaysAgo);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        handleChunkUnload(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return; // newly generated chunk has no prior history
        handleChunkLoad(event.getChunk());
    }
}
