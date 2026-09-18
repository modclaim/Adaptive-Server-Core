package net.modclaim.asc.core.mobcap;

import net.modclaim.asc.api.event.MobCapAdjustEvent;
import net.modclaim.asc.api.mobcap.LoadBudget;
import net.modclaim.asc.api.mobcap.MobCapService;
import net.modclaim.asc.api.mobcap.MobCategory;
import net.modclaim.asc.api.mobcap.MobCost;
import net.modclaim.asc.core.budget.LoadBudgetCalculator;
import net.modclaim.asc.core.metrics.ServerMetricsTracker;
import net.modclaim.asc.core.scheduler.ServerSchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of MobCapService with real-time budget scaling, despawn scanning, and admin HUD.
 */
public final class DefaultMobCapService implements MobCapService, Listener {

    private final Plugin plugin;
    private final ServerMetricsTracker metricsTracker;
    private final ServerSchedulerAdapter scheduler;
    private final LoadBudgetCalculator budgetCalculator;

    private boolean enabled = true;
    private volatile LoadBudget currentBudget;

    private final Map<EntityType, MobCost> mobCostMap = new ConcurrentHashMap<>();
    private final Set<UUID> activeMonitors = ConcurrentHashMap.newKeySet();
    private BossBar monitorBossBar;

    public DefaultMobCapService(
            @NotNull Plugin plugin,
            @NotNull ServerMetricsTracker metricsTracker,
            @NotNull ServerSchedulerAdapter scheduler
    ) {
        this.plugin = plugin;
        this.metricsTracker = metricsTracker;
        this.scheduler = scheduler;
        this.budgetCalculator = new LoadBudgetCalculator();
        initDefaultCosts();
    }

    private void initDefaultCosts() {
        for (EntityType type : EntityType.values()) {
            if (!type.isAlive()) continue;
            MobCategory category = categorizeEntity(type);
            mobCostMap.put(type, new MobCost(type, category, category.getBaseCost()));
        }
    }

    private MobCategory categorizeEntity(EntityType type) {
        String name = type.name();
        if (type == EntityType.VILLAGER || type == EntityType.WANDERING_TRADER) {
            return MobCategory.VILLAGER;
        }
        if (name.contains("BAT") || name.contains("ALLAY")) {
            return MobCategory.AMBIENT;
        }
        if (name.contains("FISH") || name.contains("SQUID") || name.contains("DOLPHIN")) {
            return MobCategory.WATER_CREATURE;
        }
        if (name.contains("AXOLOTL")) {
            return MobCategory.AXOLOTLS;
        }
        if (name.contains("COW") || name.contains("SHEEP") || name.contains("PIG") || name.contains("CHICKEN") ||
                name.contains("HORSE") || name.contains("DONKEY") || name.contains("MULE") || name.contains("RABBIT")) {
            return MobCategory.CREATURE;
        }
        // Check if monster / hostile
        try {
            Class<?> entityClass = type.getEntityClass();
            if (entityClass != null && Monster.class.isAssignableFrom(entityClass)) {
                return MobCategory.MONSTER;
            }
        } catch (Throwable ignored) {}

        return MobCategory.MISC;
    }

    private boolean initialized = false;

    @Override
    public void enable() {
        this.enabled = true;
        if (!initialized) {
            this.initialized = true;
            recalculateBudget();
            this.monitorBossBar = Bukkit.createBossBar(
                    ChatColor.GOLD + "ASC Performance Monitor",
                    BarColor.GREEN,
                    BarStyle.SOLID
            );

            // Schedule periodic budget recalculation (every 20 ticks = 1 second)
            scheduler.runAsyncTimer(this::recalculateBudget, 1000L, 1000L);

            // Schedule monitor HUD refresh (every 20 ticks = 1 second)
            scheduler.runAsyncTimer(this::updateMonitorHud, 1000L, 1000L);

            // Schedule safe despawn sweep (every 200 ticks = 10 seconds)
            scheduler.runAsyncTimer(this::runDespawnSweep, 5000L, 10000L);

            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (monitorBossBar != null) {
            monitorBossBar.removeAll();
        }
        activeMonitors.clear();
    }

    @Override
    public void reload() {
        recalculateBudget();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return "MobCapService";
    }

    private double lastDispatchedMultiplier = -1.0;
    private final Map<String, Integer> cachedCategoryCounts = new ConcurrentHashMap<>();

    private void recalculateBudget() {
        double tps = metricsTracker.getTps();
        double mspt = metricsTracker.getMspt();
        int players = Bukkit.getOnlinePlayers().size();

        LoadBudget newBudget = budgetCalculator.calculateBudget(tps, mspt, players);
        this.currentBudget = newBudget;

        // Only fire adjustment event synchronously when the budget tier actually changes significantly (>5%)
        if (Math.abs(newBudget.getBudgetMultiplier() - lastDispatchedMultiplier) >= 0.05) {
            this.lastDispatchedMultiplier = newBudget.getBudgetMultiplier();
            scheduler.runSync(() -> {
                for (World world : Bukkit.getWorlds()) {
                    for (MobCategory cat : MobCategory.values()) {
                        int cap = newBudget.getCap(cat);
                        MobCapAdjustEvent event = new MobCapAdjustEvent(world, cat, newBudget, cap);
                        Bukkit.getPluginManager().callEvent(event);
                    }
                }
            });
        }
    }

    @Override
    @NotNull
    public LoadBudget getCurrentBudget() {
        if (currentBudget == null) {
            recalculateBudget();
        }
        return currentBudget;
    }

    @Override
    public int getEffectiveCap(@NotNull World world, @NotNull MobCategory category) {
        return getCurrentBudget().getCap(category);
    }

    @Override
    public boolean canSpawn(@NotNull World world, @NotNull EntityType type, @Nullable String regionId) {
        if (!enabled) return true;

        // Under normal healthy load (MSPT < 38.0), allow vanilla spawning with zero overhead
        if (metricsTracker.getMspt() < 38.0) {
            return true;
        }

        MobCost cost = mobCostMap.get(type);
        MobCategory category = (cost != null) ? cost.getCategory() : MobCategory.MISC;
        int maxCap = getEffectiveCap(world, category);

        // Check against fast cached count instead of heavy full-world entity copying
        String cacheKey = world.getName() + ":" + category.name();
        int currentCount = cachedCategoryCounts.getOrDefault(cacheKey, 0);
        return currentCount < maxCap;
    }

    @Override
    public void registerMobCost(@NotNull MobCost cost) {
        mobCostMap.put(cost.getEntityType(), cost);
    }

    @Override
    public boolean isProtectedFromDespawn(@NotNull Entity entity) {
        return MobDespawnPolicy.isProtectedFromDespawn(entity);
    }

    @Override
    public boolean toggleMonitor(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (activeMonitors.contains(uuid)) {
            activeMonitors.remove(uuid);
            if (monitorBossBar != null) {
                monitorBossBar.removePlayer(player);
            }
            player.sendMessage(ChatColor.YELLOW + "[ASC] Mob Cap & Performance monitor disabled.");
            return false;
        } else {
            activeMonitors.add(uuid);
            if (monitorBossBar != null) {
                monitorBossBar.addPlayer(player);
            }
            player.sendMessage(ChatColor.GREEN + "[ASC] Mob Cap & Performance monitor enabled.");
            return true;
        }
    }

    @Override
    public boolean isMonitoring(@NotNull Player player) {
        return activeMonitors.contains(player.getUniqueId());
    }

    private void updateMonitorHud() {
        if (activeMonitors.isEmpty() || monitorBossBar == null) return;

        LoadBudget budget = getCurrentBudget();
        double tps = budget.getTps();
        double mspt = budget.getMspt();
        double multiplier = budget.getBudgetMultiplier();

        ChatColor tpsColor = (tps >= 19.0) ? ChatColor.GREEN : (tps >= 15.0 ? ChatColor.YELLOW : ChatColor.RED);
        ChatColor msptColor = (mspt <= 35.0) ? ChatColor.GREEN : (mspt <= 45.0 ? ChatColor.YELLOW : ChatColor.RED);

        String title = String.format(
                "%sTPS: %s%.1f §8| %sMSPT: %s%.1fms §8| §eBudget: §f%.0f%% §8| §6Entities: §f%d",
                ChatColor.GRAY, tpsColor, tps,
                ChatColor.GRAY, msptColor, mspt,
                multiplier * 100.0,
                metricsTracker.getTotalEntities()
        );

        BarColor barColor = (tps >= 19.0) ? BarColor.GREEN : (tps >= 15.0 ? BarColor.YELLOW : BarColor.RED);
        double progress = Math.min(1.0, Math.max(0.0, multiplier / 1.5));

        scheduler.runSync(() -> {
            monitorBossBar.setTitle(title);
            monitorBossBar.setColor(barColor);
            monitorBossBar.setProgress(progress);

            for (UUID uuid : activeMonitors) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.sendActionBar(ChatColor.DARK_AQUA + "ASC HUD: " + title);
                } else {
                    activeMonitors.remove(uuid);
                }
            }
        });
    }

    private void runDespawnSweep() {
        if (!enabled || currentBudget == null || currentBudget.getBudgetMultiplier() >= 0.95) {
            // Under normal healthy load, let vanilla Minecraft despawn handle entities
            return;
        }

        scheduler.runSync(() -> {
            int pruned = 0;
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (!(entity instanceof Mob mob)) continue;
                    if (isProtectedFromDespawn(mob)) continue;

                    // Check distance to closest player
                    boolean hasNearbyPlayer = false;
                    for (Player player : world.getPlayers()) {
                        if (player.getLocation().distanceSquared(mob.getLocation()) < (72 * 72)) {
                            hasNearbyPlayer = true;
                            break;
                        }
                    }

                    if (!hasNearbyPlayer && mob.getTicksLived() > 600) {
                        mob.remove();
                        pruned++;
                        if (pruned >= 50) break; // Limit prune batch per sweep
                    }
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!enabled) return;

        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        // Only throttle natural and ambient world spawns; never interfere with CHUNK_GEN or custom spawns
        if (reason == CreatureSpawnEvent.SpawnReason.NATURAL ||
                reason == CreatureSpawnEvent.SpawnReason.DEFAULT) {

            if (!canSpawn(event.getLocation().getWorld(), event.getEntityType(), null)) {
                event.setCancelled(true);
            }
        }
    }
}
