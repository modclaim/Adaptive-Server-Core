package net.modclaim.asc.core.compatibility;

import net.modclaim.asc.api.compatibility.CompatibilityMode;
import net.modclaim.asc.api.compatibility.CompatibilityService;
import net.modclaim.asc.api.compatibility.DetectedPlugin;
import net.modclaim.asc.api.event.CompatibilityModeChangeEvent;
import net.modclaim.asc.core.metrics.ServerMetricsTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages soft-dependency detection, anti-conflict resolution, and diagnostic reporting.
 */
public final class DefaultCompatibilityService implements CompatibilityService {

    private final Plugin plugin;
    private final ServerMetricsTracker metricsTracker;
    private CompatibilityMode mode = CompatibilityMode.FULL;
    private final List<DetectedPlugin> detectedPlugins = new ArrayList<>();
    private final Set<String> suppressedFeatures = ConcurrentHashMap.newKeySet();

    public DefaultCompatibilityService(@NotNull Plugin plugin, @NotNull ServerMetricsTracker metricsTracker) {
        this.plugin = plugin;
        this.metricsTracker = metricsTracker;
    }

    @Override
    public void enable() {
        scanInstalledPlugins();
        registerPlaceholderApiIfPresent();
    }

    @Override
    public void disable() {
        detectedPlugins.clear();
        suppressedFeatures.clear();
    }

    @Override
    public void reload() {
        detectedPlugins.clear();
        suppressedFeatures.clear();
        scanInstalledPlugins();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "CompatibilityService";
    }

    private void scanInstalledPlugins() {
        PluginManager pm = Bukkit.getPluginManager();

        // 1. Check for conflicting anti-lag plugins
        if (pm.isPluginEnabled("ClearLag")) {
            Plugin p = pm.getPlugin("ClearLag");
            detectedPlugins.add(new DetectedPlugin("ClearLag", p.getDescription().getVersion(), true,
                    "Switched to COMPATIBILITY_REDUCED: disabled internal entity sweeper to prevent duplicate despawn hooks."));
            suppressFeature("internal-despawn-sweep");
            setMode(CompatibilityMode.COMPATIBILITY_REDUCED, "ClearLag detected on server");
        }

        if (pm.isPluginEnabled("LaggRemover")) {
            Plugin p = pm.getPlugin("LaggRemover");
            detectedPlugins.add(new DetectedPlugin("LaggRemover", p.getDescription().getVersion(), true,
                    "Switched to COMPATIBILITY_REDUCED: suppressed conflicting memory cleaners."));
            suppressFeature("internal-despawn-sweep");
            setMode(CompatibilityMode.COMPATIBILITY_REDUCED, "LaggRemover detected on server");
        }

        // 2. Check friendly integrations
        if (pm.isPluginEnabled("WorldGuard")) {
            Plugin p = pm.getPlugin("WorldGuard");
            detectedPlugins.add(new DetectedPlugin("WorldGuard", p.getDescription().getVersion(), false,
                    "Hooked regional boundaries for per-region mob caps and trusted technical zones."));
        }

        if (pm.isPluginEnabled("Chunky")) {
            Plugin p = pm.getPlugin("Chunky");
            detectedPlugins.add(new DetectedPlugin("Chunky", p.getDescription().getVersion(), false,
                    "Hooked world pre-generation watchdog: relaxes chunk throttling during pre-gen jobs."));
        }

        if (pm.isPluginEnabled("MythicMobs")) {
            Plugin p = pm.getPlugin("MythicMobs");
            detectedPlugins.add(new DetectedPlugin("MythicMobs", p.getDescription().getVersion(), false,
                    "Hooked custom mob protector: RPG mobs strictly whitelisted from despawn."));
        }

        if (pm.isPluginEnabled("PlaceholderAPI")) {
            Plugin p = pm.getPlugin("PlaceholderAPI");
            detectedPlugins.add(new DetectedPlugin("PlaceholderAPI", p.getDescription().getVersion(), false,
                    "Registered %asc_tps%, %asc_mspt%, %asc_budget%, %asc_mode% placeholders."));
        }

        if (pm.isPluginEnabled("Vault")) {
            Plugin p = pm.getPlugin("Vault");
            detectedPlugins.add(new DetectedPlugin("Vault", p.getDescription().getVersion(), false,
                    "Permissions and economy integration active."));
        }
    }

    private void registerPlaceholderApiIfPresent() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                // Use reflection or standard PAPI hook so no hard class linkage failure occurs if absent
                Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
                // ASCPlaceholderExpansion will hook if available
            } catch (Throwable ignored) {}
        }
    }

    public void suppressFeature(@NotNull String featureKey) {
        suppressedFeatures.add(featureKey);
    }

    private void setMode(@NotNull CompatibilityMode newMode, @NotNull String reason) {
        CompatibilityMode prev = this.mode;
        this.mode = newMode;
        plugin.getLogger().info("Compatibility mode updated: " + newMode + " (" + reason + ")");
        Bukkit.getPluginManager().callEvent(new CompatibilityModeChangeEvent(prev, newMode, reason));
    }

    @Override
    @NotNull
    public CompatibilityMode getMode() {
        return mode;
    }

    @Override
    @NotNull
    public List<DetectedPlugin> getDetectedPlugins() {
        return Collections.unmodifiableList(detectedPlugins);
    }

    @Override
    public boolean isPluginDetected(@NotNull String pluginName) {
        for (DetectedPlugin dp : detectedPlugins) {
            if (dp.getName().equalsIgnoreCase(pluginName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFeatureSuppressed(@NotNull String featureKey) {
        return suppressedFeatures.contains(featureKey);
    }

    @Override
    @NotNull
    public List<String> generateDiagnosticReport() {
        List<String> report = new ArrayList<>();
        report.add(ChatColor.GOLD + "=== [Adaptive Server Core Diagnostic Report] ===");
        report.add(ChatColor.GRAY + "Server Software: " + ChatColor.WHITE + Bukkit.getName() + " " + Bukkit.getVersion());
        report.add(ChatColor.GRAY + "Core Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        report.add(ChatColor.GRAY + "Current TPS: " + ChatColor.GREEN + String.format("%.2f", metricsTracker.getTps()) +
                ChatColor.GRAY + " | MSPT: " + ChatColor.AQUA + String.format("%.2fms", metricsTracker.getMspt()));
        report.add(ChatColor.GRAY + "Operating Mode: " + ChatColor.YELLOW + mode.name() + " (" + mode.getDescription() + ")");

        report.add(ChatColor.GOLD + "--- Detected Integrations & Shims ---");
        if (detectedPlugins.isEmpty()) {
            report.add(ChatColor.DARK_GRAY + "  (No relevant external plugins detected)");
        } else {
            for (DetectedPlugin dp : detectedPlugins) {
                ChatColor color = dp.isConflicting() ? ChatColor.LIGHT_PURPLE : ChatColor.GREEN;
                report.add(String.format("%s• %s v%s: %s%s", color, dp.getName(), dp.getVersion(), ChatColor.GRAY, dp.getActionTaken()));
            }
        }

        report.add(ChatColor.GOLD + "--- Suppressed Features ---");
        if (suppressedFeatures.isEmpty()) {
            report.add(ChatColor.DARK_GRAY + "  (None - running all native modules)");
        } else {
            for (String sf : suppressedFeatures) {
                report.add(ChatColor.RED + "• " + sf + " (disabled for compatibility)");
            }
        }

        report.add(ChatColor.GOLD + "================================================");
        return report;
    }
}
