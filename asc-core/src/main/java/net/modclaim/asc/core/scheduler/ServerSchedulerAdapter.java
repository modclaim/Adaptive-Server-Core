package net.modclaim.asc.core.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Universal scheduler abstraction bridging standard Bukkit, Paper, and Folia regionized schedulers.
 */
public final class ServerSchedulerAdapter {

    private final Plugin plugin;
    private final boolean isFolia;
    private final ScheduledExecutorService asyncExecutor;

    private Object foliaRegionScheduler;
    private Object foliaGlobalRegionScheduler;
    private Object foliaAsyncScheduler;

    private Method foliaRegionExecuteChunk;
    private Method foliaGlobalExecute;
    private Method foliaAsyncRunNow;

    public ServerSchedulerAdapter(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.isFolia = checkFolia();
        this.asyncExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "ASC-AsyncWorker");
            t.setDaemon(true);
            return t;
        });

        if (isFolia) {
            initFoliaReflections();
        }
    }

    private boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void initFoliaReflections() {
        try {
            Method getRegionSched = Bukkit.class.getMethod("getRegionScheduler");
            this.foliaRegionScheduler = getRegionSched.invoke(null);

            Method getGlobalSched = Bukkit.class.getMethod("getGlobalRegionScheduler");
            this.foliaGlobalRegionScheduler = getGlobalSched.invoke(null);

            Method getAsyncSched = Bukkit.class.getMethod("getAsyncScheduler");
            this.foliaAsyncScheduler = getAsyncSched.invoke(null);

            this.foliaRegionExecuteChunk = foliaRegionScheduler.getClass().getMethod(
                    "execute", Plugin.class, World.class, int.class, int.class, Runnable.class
            );
            this.foliaGlobalExecute = foliaGlobalRegionScheduler.getClass().getMethod(
                    "execute", Plugin.class, Runnable.class
            );
            this.foliaAsyncRunNow = foliaAsyncScheduler.getClass().getMethod(
                    "runNow", Plugin.class, java.util.function.Consumer.class
            );
            plugin.getLogger().info("Folia multi-threaded region scheduler successfully hooked!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Detected Folia server but failed to bind reflection schedulers", e);
        }
    }

    public boolean isFolia() {
        return isFolia;
    }

    public void runAsync(@NotNull Runnable task) {
        asyncExecutor.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Error executing async task in ASC", t);
            }
        });
    }

    public void runAsyncTimer(@NotNull Runnable task, long delayMs, long periodMs) {
        asyncExecutor.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Error executing scheduled async task in ASC", t);
            }
        }, delayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public void runSync(@NotNull Runnable task) {
        if (isFolia && foliaGlobalRegionScheduler != null && foliaGlobalExecute != null) {
            try {
                foliaGlobalExecute.invoke(foliaGlobalRegionScheduler, plugin, task);
                return;
            } catch (Exception ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runAtChunk(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable task) {
        if (isFolia && foliaRegionScheduler != null && foliaRegionExecuteChunk != null) {
            try {
                foliaRegionExecuteChunk.invoke(foliaRegionScheduler, plugin, world, chunkX, chunkZ, task);
                return;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error dispatching task to Folia region scheduler", e);
            }
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void runAtLocation(@NotNull Location location, @NotNull Runnable task) {
        World world = location.getWorld();
        if (world == null) return;
        runAtChunk(world, location.getBlockX() >> 4, location.getBlockZ() >> 4, task);
    }

    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
