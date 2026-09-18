package net.modclaim.asc.core;

import net.modclaim.asc.api.ASCPlugin;
import net.modclaim.asc.api.ASCProvider;
import net.modclaim.asc.api.chunk.ChunkThrottleService;
import net.modclaim.asc.api.compatibility.CompatibilityService;
import net.modclaim.asc.api.lazysim.LazySimService;
import net.modclaim.asc.api.mobcap.MobCapService;
import net.modclaim.asc.api.redstone.RedstoneWatchdogService;
import net.modclaim.asc.core.command.ASCCommand;
import net.modclaim.asc.core.command.ASCTabCompleter;
import net.modclaim.asc.core.compatibility.ASCPlaceholderExpansion;
import net.modclaim.asc.core.compatibility.DefaultCompatibilityService;
import net.modclaim.asc.core.config.ConfigManager;
import net.modclaim.asc.core.config.ProfileManager;
import net.modclaim.asc.core.mobcap.DefaultMobCapService;
import net.modclaim.asc.core.metrics.ServerMetricsTracker;
import net.modclaim.asc.core.persistence.DatabaseManager;
import net.modclaim.asc.core.redstone.DefaultRedstoneWatchdogService;
import net.modclaim.asc.core.scheduler.ServerSchedulerAdapter;
import net.modclaim.asc.core.chunk.DefaultChunkThrottleService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Reusable core plugin lifecycle controller shared across all platform adapters and the universal loader.
 */
public final class ASCPluginController {

    private final JavaPlugin plugin;
    private final String platformName;

    private ServerSchedulerAdapter scheduler;
    private ServerMetricsTracker metricsTracker;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private ProfileManager profileManager;

    private DefaultMobCapService mobCapService;
    private DefaultRedstoneWatchdogService redstoneWatchdog;
    private DefaultChunkThrottleService chunkThrottleService;
    private LazySimService lazySimService;
    private DefaultCompatibilityService compatibilityService;

    public ASCPluginController(@NotNull JavaPlugin plugin, @NotNull String platformName) {
        this.plugin = plugin;
        this.platformName = platformName;
    }

    public void enable() {
        plugin.getLogger().info("Initializing Adaptive Server Core (ASC) on platform: " + platformName + "...");

        // 1. Initialize core infrastructure
        this.scheduler = new ServerSchedulerAdapter(plugin);
        this.metricsTracker = new ServerMetricsTracker(plugin);
        this.databaseManager = new DatabaseManager(plugin);
        this.databaseManager.initialize();

        this.configManager = new ConfigManager(plugin);
        this.configManager.load();

        this.profileManager = new ProfileManager(plugin, databaseManager);

        // Schedule tick recording on main scheduler (every 1 tick)
        Bukkit.getScheduler().runTaskTimer(plugin, metricsTracker::recordTick, 1L, 1L);

        // 2. Initialize and enable modular services
        this.mobCapService = new DefaultMobCapService(plugin, metricsTracker, scheduler);
        this.mobCapService.enable();

        this.redstoneWatchdog = new DefaultRedstoneWatchdogService(plugin, scheduler);
        this.redstoneWatchdog.enable();

        this.chunkThrottleService = new DefaultChunkThrottleService(plugin, metricsTracker, scheduler);
        this.chunkThrottleService.enable();

        this.compatibilityService = new DefaultCompatibilityService(plugin, metricsTracker);
        this.compatibilityService.enable();

        // Dynamically instantiate LazySimService from asc-lazysim if on classpath
        initLazySimService();

        // 3. Register Commands & Tab Completers
        PluginCommand cmd = plugin.getCommand("asc");
        if (cmd != null) {
            ASCCommand executor = new ASCCommand((ASCPlugin) plugin, profileManager);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(new ASCTabCompleter(profileManager));
        }

        // 4. Register PlaceholderAPI expansion if installed
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                new ASCPlaceholderExpansion((ASCPlugin) plugin).register();
                plugin.getLogger().info("Registered PlaceholderAPI expansion successfully.");
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not register PlaceholderAPI expansion: " + t.getMessage());
            }
        }

        // 5. Register with global provider
        ASCProvider.register((ASCPlugin) plugin);

        plugin.getLogger().info("Adaptive Server Core (ASC) v" + plugin.getDescription().getVersion() + " successfully enabled!");
    }

    private void initLazySimService() {
        try {
            Class<?> lazySimClass = Class.forName("net.modclaim.asc.lazysim.DefaultLazySimService");
            this.lazySimService = (LazySimService) lazySimClass
                    .getConstructor(org.bukkit.plugin.Plugin.class, DatabaseManager.class, ServerSchedulerAdapter.class)
                    .newInstance(plugin, databaseManager, scheduler);
            this.lazySimService.enable();
            plugin.getLogger().info("Lazy Simulation Engine initialized and enabled!");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "LazySim module could not be initialized. Operating in fallback mode.", t);
        }
    }

    public void disable() {
        plugin.getLogger().info("Disabling Adaptive Server Core...");

        if (mobCapService != null) mobCapService.disable();
        if (redstoneWatchdog != null) redstoneWatchdog.disable();
        if (chunkThrottleService != null) chunkThrottleService.disable();
        if (lazySimService != null) lazySimService.disable();
        if (compatibilityService != null) compatibilityService.disable();

        if (databaseManager != null) databaseManager.close();
        if (scheduler != null) scheduler.shutdown();

        ASCProvider.register(null);
        plugin.getLogger().info("Adaptive Server Core disabled.");
    }

    public ServerSchedulerAdapter getScheduler() {
        return scheduler;
    }

    public ServerMetricsTracker getMetricsTracker() {
        return metricsTracker;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public MobCapService getMobCapService() {
        return mobCapService;
    }

    public RedstoneWatchdogService getRedstoneWatchdog() {
        return redstoneWatchdog;
    }

    public ChunkThrottleService getChunkThrottleService() {
        return chunkThrottleService;
    }

    public LazySimService getLazySimService() {
        return lazySimService;
    }

    public CompatibilityService getCompatibilityService() {
        return compatibilityService;
    }

    public String getPlatformName() {
        return platformName;
    }
}
