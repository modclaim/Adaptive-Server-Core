package net.modclaim.asc.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Manages plugin configuration loading, saving, and per-world overrides.
 */
public final class ConfigManager {

    private final Plugin plugin;
    private FileConfiguration config;
    private final File configFile;

    public ConfigManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        load();
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }

    // MobCap settings
    public boolean isMobCapEnabled() {
        return getConfig().getBoolean("mobcap.enabled", true);
    }

    public double getTargetMspt() {
        return getConfig().getDouble("mobcap.target-mspt", 40.0);
    }

    public double getTargetTps() {
        return getConfig().getDouble("mobcap.target-tps", 20.0);
    }

    public double getMaxBonusMultiplier() {
        return getConfig().getDouble("mobcap.max-bonus-multiplier", 1.5);
    }

    public double getMinFloorMultiplier() {
        return getConfig().getDouble("mobcap.min-floor-multiplier", 0.25);
    }

    // Redstone settings
    public boolean isRedstoneWatchdogEnabled() {
        return getConfig().getBoolean("redstone-watchdog.enabled", true);
    }

    public int getRedstoneFrequencyThreshold() {
        return getConfig().getInt("redstone-watchdog.frequency-threshold", 16);
    }

    public int getHopperTransferLimit() {
        return getConfig().getInt("redstone-watchdog.hopper-transfer-limit", 50);
    }

    // Chunk / Elytra settings
    public boolean isChunkThrottleEnabled() {
        return getConfig().getBoolean("chunk-throttle.enabled", true);
    }

    public int getMinViewDistance() {
        return getConfig().getInt("chunk-throttle.min-view-distance", 6);
    }

    public int getMaxViewDistance() {
        return getConfig().getInt("chunk-throttle.max-view-distance", 12);
    }

    // LazySim settings
    public boolean isLazySimEnabled() {
        return getConfig().getBoolean("lazysim.enabled", true);
    }

    public long getLazySimMaxCatchUpHours() {
        return getConfig().getLong("lazysim.max-catchup-hours", 168L); // 7 days
    }

    public boolean isLazySimFurnacesEnabled() {
        return getConfig().getBoolean("lazysim.processors.furnaces", true);
    }

    public boolean isLazySimCropsEnabled() {
        return getConfig().getBoolean("lazysim.processors.crops", true);
    }

    public boolean isLazySimVillagersEnabled() {
        return getConfig().getBoolean("lazysim.processors.villagers", true);
    }

    public boolean isLazySimAnimalsEnabled() {
        return getConfig().getBoolean("lazysim.processors.animals", true);
    }

    public boolean isLazySimBeehivesEnabled() {
        return getConfig().getBoolean("lazysim.processors.beehives", true);
    }
}
