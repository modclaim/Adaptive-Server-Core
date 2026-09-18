package net.modclaim.asc.core.gui;

import net.modclaim.asc.api.ASCPlugin;
import net.modclaim.asc.api.mobcap.LoadBudget;
import net.modclaim.asc.api.redstone.LagSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import net.modclaim.asc.core.scheduler.ServerSchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * In-game 54-slot chest GUI providing interactive controls for modules, budgets, and lag inspection.
 * Uses dedicated ASCGuiHolder to strictly prevent item picking/moving across all server platforms.
 */
public final class ASCAdminGUI implements Listener {

    public static final class ASCGuiHolder implements InventoryHolder {
        private Inventory inventory;

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory != null ? inventory : Bukkit.createInventory(null, 54);
        }
    }

    private static final String GUI_TITLE = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "ASC Core Control Panel";
    private final ASCPlugin plugin;
    private final ServerSchedulerAdapter scheduler;

    public ASCAdminGUI(@NotNull ASCPlugin plugin, @NotNull ServerSchedulerAdapter scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public ASCAdminGUI(@NotNull ASCPlugin plugin) {
        this(plugin, new ServerSchedulerAdapter(plugin));
    }

    public void open(@NotNull Player player) {
        ASCGuiHolder holder = new ASCGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, GUI_TITLE);
        holder.setInventory(inv);

        render(inv);
        player.openInventory(inv);
        playOpenSound(player);
    }

    /**
     * Renders or refreshes all items in the 54-slot GUI.
     */
    public void render(@NotNull Inventory inv) {
        // 1. Fill background with dark gray panes
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        // Decorative borders on row 1
        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 9; i < 18; i++) {
            inv.setItem(i, border);
        }

        // 2. Slot 4: Performance Status (Nether Star)
        LoadBudget budget = plugin.getMobCapService().getCurrentBudget();
        ChatColor tpsColor = budget.getTps() >= 19.0 ? ChatColor.GREEN : (budget.getTps() >= 15.0 ? ChatColor.YELLOW : ChatColor.RED);
        ItemStack statusItem = createItem(
                Material.NETHER_STAR,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Server Performance Status",
                ChatColor.GRAY + "Current TPS: " + tpsColor + String.format("%.2f", budget.getTps()),
                ChatColor.GRAY + "Current MSPT: " + ChatColor.AQUA + String.format("%.2fms", budget.getMspt()),
                ChatColor.GRAY + "Load Budget: " + ChatColor.YELLOW + String.format("%.0f%%", budget.getBudgetMultiplier() * 100.0),
                ChatColor.GRAY + "Active Mode: " + ChatColor.WHITE + plugin.getCompatibilityService().getMode().name(),
                ChatColor.GRAY + "Tracked Chunks: " + ChatColor.GREEN + plugin.getLazySimService().getTrackedHibernatingCount(),
                "",
                ChatColor.DARK_GRAY + "Real-time PID adaptive control active"
        );
        inv.setItem(4, statusItem);

        // 3. ROW 2 (Slots 19, 21, 23, 25): Intuitive Minecraft Feature Icons
        // Slot 19: MobCap (Spawner icon)
        inv.setItem(19, createItem(
                Material.SPAWNER,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "MobCap & Entity Limiter",
                ChatColor.GRAY + "Dynamic load-budget mob scaling.",
                ChatColor.GRAY + "Safeguards named mobs, villagers & pets.",
                "",
                ChatColor.AQUA + "▶ Click icon or switch below to toggle"
        ));

        // Slot 21: Redstone Watchdog (Redstone Repeater)
        inv.setItem(21, createItem(
                Material.REPEATER,
                ChatColor.RED + "" + ChatColor.BOLD + "Redstone & Clock Watchdog",
                ChatColor.GRAY + "Real-time loop detection & clock suppression.",
                ChatColor.GRAY + "Prevents lag machines and hopper overloads.",
                "",
                ChatColor.AQUA + "▶ Click icon or switch below to toggle"
        ));

        // Slot 23: Chunk / Elytra Throttle (Elytra)
        inv.setItem(23, createItem(
                Material.ELYTRA,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Elytra & Chunk Throttle",
                ChatColor.GRAY + "Fair-share CPU budget for speed flight.",
                ChatColor.GRAY + "Eliminates server freeze on rapid exploration.",
                "",
                ChatColor.AQUA + "▶ Click icon or switch below to toggle"
        ));

        // Slot 25: Lazy Simulation Engine (Furnace)
        inv.setItem(25, createItem(
                Material.FURNACE,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Lazy Simulation Engine",
                ChatColor.GRAY + "Delta catch-up for crops, furnaces & villagers.",
                ChatColor.GRAY + "Allows chunk hibernation with zero player loss.",
                "",
                ChatColor.AQUA + "▶ Click icon or switch below to toggle"
        ));

        // 4. ROW 3 (Slots 28, 30, 32, 34): Dedicated Green / Red Toggle Blocks underneath each item!
        // Slot 28: MobCap Toggle Switch
        boolean mobCapOn = plugin.getMobCapService().isEnabled();
        inv.setItem(28, createToggleItem(
                mobCapOn,
                "MobCap Service",
                "Dynamic entity load-budget scaling"
        ));

        // Slot 30: Redstone Toggle Switch
        boolean redstoneOn = plugin.getRedstoneWatchdog().isEnabled();
        inv.setItem(30, createToggleItem(
                redstoneOn,
                "Redstone Watchdog",
                "Loop suppressor & clock throttle"
        ));

        // Slot 32: Chunk Throttle Toggle Switch
        boolean chunkOn = plugin.getChunkThrottleService().isEnabled();
        inv.setItem(32, createToggleItem(
                chunkOn,
                "Chunk & Elytra Throttle",
                "Speed flight chunk load balancer"
        ));

        // Slot 34: Lazy Sim Toggle Switch
        boolean lazyOn = plugin.getLazySimService().isEnabled();
        inv.setItem(34, createToggleItem(
                lazyOn,
                "Lazy Simulation",
                "Delta catch-up on chunk awaken"
        ));

        // 5. Slot 40: Top Lag Sources Viewer (Compass)
        List<LagSource> lagSources = plugin.getRedstoneWatchdog().getTopLagSources(3);
        List<String> lagLore = new ArrayList<>();
        lagLore.add(ChatColor.GRAY + "Heaviest current chunk regions:");
        if (lagSources.isEmpty()) {
            lagLore.add(ChatColor.DARK_GRAY + "  (No heavy lag sources detected)");
        } else {
            for (LagSource ls : lagSources) {
                lagLore.add(String.format(
                        "§e• %s [%d, %d]: §c~%.1fms §7(RS:%d, Hop:%d, Ent:%d)",
                        ls.getWorldName(), ls.getChunkX(), ls.getChunkZ(),
                        ls.getEstimatedMsptImpact(),
                        ls.getRedstoneUpdatesPerSec(), ls.getHopperTransfersPerSec(), ls.getEntityCount()
                ));
            }
        }
        lagLore.add("");
        lagLore.add(ChatColor.YELLOW + "Click to refresh diagnostics");
        inv.setItem(40, createItem(Material.COMPASS, ChatColor.GOLD + "" + ChatColor.BOLD + "Top Lag Sources", lagLore.toArray(new String[0])));

        // 6. Action buttons on bottom row
        // Slot 48: Hot Reload Config
        inv.setItem(48, createItem(Material.BLAZE_POWDER, ChatColor.YELLOW + "" + ChatColor.BOLD + "Hot Reload Config",
                ChatColor.GRAY + "Reloads config.yml and all profiles.",
                "",
                ChatColor.YELLOW + "▶ Click to reload"
        ));

        // Slot 50: Close Menu
        inv.setItem(50, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Close Menu",
                ChatColor.GRAY + "Exit control panel.",
                "",
                ChatColor.RED + "▶ Click to close"
        ));
    }

    private static ItemStack createToggleItem(boolean enabled, String featureName, String description) {
        Material mat = enabled ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        String statusText = enabled ? (ChatColor.GREEN + "[ ACTIVE ]") : (ChatColor.RED + "[ DISABLED ]");
        String actionText = enabled ? (ChatColor.RED + "▶ Click to Turn OFF") : (ChatColor.GREEN + "▶ Click to Turn ON");

        return createItem(
                mat,
                statusText + " " + ChatColor.WHITE + featureName,
                ChatColor.GRAY + description,
                "",
                ChatColor.GRAY + "Status: " + (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"),
                actionText
        );
    }

    private static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null) return;

        // Verify if this inventory is the ASC Control Panel via Holder or title fallback
        boolean isAsc = (topInv.getHolder() instanceof ASCGuiHolder);
        if (!isAsc) {
            try {
                String title = event.getView().getTitle();
                if (title != null && ChatColor.stripColor(title).contains("ASC Core Control Panel")) {
                    isAsc = true;
                }
            } catch (Throwable ignored) {}
        }

        if (!isAsc) return;

        // 1. ALWAYS cancel and deny click across all versions to prevent taking or moving items
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Ignore clicks outside or in player inventory (while keeping them strictly cancelled)
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= 54) {
            return;
        }

        boolean toggled = false;

        // Handle slots: Icon (Row 2) OR Toggle Switch (Row 3)
        if (rawSlot == 19 || rawSlot == 28) {
            // MobCap Service
            boolean newState = !plugin.getMobCapService().isEnabled();
            if (newState) {
                plugin.getMobCapService().enable();
            } else {
                plugin.getMobCapService().disable();
            }
            try {
                plugin.getConfig().set("modules.mobcap.enabled", newState);
                plugin.saveConfig();
            } catch (Throwable ignored) {}

            sendToggleFeedback(player, "MobCap Service", newState);
            toggled = true;
        } else if (rawSlot == 21 || rawSlot == 30) {
            // Redstone Watchdog
            boolean newState = !plugin.getRedstoneWatchdog().isEnabled();
            if (newState) {
                plugin.getRedstoneWatchdog().enable();
            } else {
                plugin.getRedstoneWatchdog().disable();
            }
            try {
                plugin.getConfig().set("modules.redstone.enabled", newState);
                plugin.saveConfig();
            } catch (Throwable ignored) {}

            sendToggleFeedback(player, "Redstone Watchdog", newState);
            toggled = true;
        } else if (rawSlot == 23 || rawSlot == 32) {
            // Chunk & Elytra Throttle
            boolean newState = !plugin.getChunkThrottleService().isEnabled();
            if (newState) {
                plugin.getChunkThrottleService().enable();
            } else {
                plugin.getChunkThrottleService().disable();
            }
            try {
                plugin.getConfig().set("modules.chunk-throttle.enabled", newState);
                plugin.saveConfig();
            } catch (Throwable ignored) {}

            sendToggleFeedback(player, "Chunk & Elytra Throttle", newState);
            toggled = true;
        } else if (rawSlot == 25 || rawSlot == 34) {
            // Lazy Sim
            boolean newState = !plugin.getLazySimService().isEnabled();
            if (newState) {
                plugin.getLazySimService().enable();
            } else {
                plugin.getLazySimService().disable();
            }
            try {
                plugin.getConfig().set("modules.lazysim.enabled", newState);
                plugin.saveConfig();
            } catch (Throwable ignored) {}

            sendToggleFeedback(player, "Lazy Simulation Engine", newState);
            toggled = true;
        } else if (rawSlot == 40) {
            // Refresh diagnostics
            playClickSound(player, 1.5f);
            player.sendActionBar(ChatColor.AQUA + "↻ [ASC] Diagnostics refreshed!");
            player.sendMessage(ChatColor.AQUA + "[ASC] Diagnostics refreshed!");
            toggled = true;
        } else if (rawSlot == 48) {
            // Hot Reload Config
            plugin.reloadConfig();
            plugin.getMobCapService().reload();
            plugin.getRedstoneWatchdog().reload();
            plugin.getCompatibilityService().reload();
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
            } catch (Throwable ignored) {}
            player.sendActionBar(ChatColor.GREEN + "✔ [ASC] Config & Profiles Hot-Reloaded!");
            player.sendMessage(ChatColor.GREEN + "[ASC] Configuration and all profiles reloaded successfully!");
            toggled = true;
        } else if (rawSlot == 50) {
            // Close
            playClickSound(player, 0.8f);
            player.closeInventory();
            return;
        }

        if (toggled) {
            // Re-render and force client visual inventory resynchronization on NEXT TICK
            scheduler.runSync(() -> {
                if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() instanceof ASCGuiHolder) {
                    render(topInv);
                    player.updateInventory();
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView() == null) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null) return;

        boolean isAsc = (topInv.getHolder() instanceof ASCGuiHolder);
        if (!isAsc) {
            try {
                String title = event.getView().getTitle();
                if (title != null && ChatColor.stripColor(title).contains("ASC Core Control Panel")) {
                    isAsc = true;
                }
            } catch (Throwable ignored) {}
        }

        if (isAsc) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    private static void sendToggleFeedback(@NotNull Player player, @NotNull String featureName, boolean newState) {
        // 1. Crisp sound feedback (Block note pling: pitch 2.0 for ON, 0.7 for OFF)
        try {
            float pitch = newState ? 2.0f : 0.7f;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
        } catch (Throwable ignored) {
            playClickSound(player, newState ? 1.4f : 0.8f);
        }

        // 2. Action Bar notification (Directly visible while chest inventory is open!)
        String statusActionBar = newState ? (ChatColor.GREEN + "✔ ENABLED") : (ChatColor.RED + "✖ DISABLED");
        player.sendActionBar(ChatColor.GOLD + "[ASC] " + ChatColor.WHITE + featureName + ": " + statusActionBar);

        // 3. Chat message
        String statusChat = newState ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED");
        player.sendMessage(ChatColor.GOLD + "[ASC] " + ChatColor.YELLOW + featureName + " is now " + statusChat);
    }

    private static void playOpenSound(@NotNull Player player) {
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
        } catch (Throwable ignored) {}
    }

    private static void playClickSound(@NotNull Player player, float pitch) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, pitch);
        } catch (Throwable ignored) {}
    }
}
