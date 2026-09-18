package net.modclaim.asc.core.gui;

import net.modclaim.asc.api.ASCPlugin;
import net.modclaim.asc.api.mobcap.LoadBudget;
import net.modclaim.asc.api.redstone.LagSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * In-game 54-slot chest GUI providing interactive controls for modules, budgets, and lag inspection.
 */
public final class ASCAdminGUI implements Listener {

    private static final String GUI_TITLE = ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "ASC Core Control Panel";
    private final ASCPlugin plugin;

    public ASCAdminGUI(@NotNull ASCPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(@NotNull Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fill background with dark gray panes
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        // Slot 4: Performance Status
        LoadBudget budget = plugin.getMobCapService().getCurrentBudget();
        ChatColor tpsColor = budget.getTps() >= 19.0 ? ChatColor.GREEN : (budget.getTps() >= 15.0 ? ChatColor.YELLOW : ChatColor.RED);
        ItemStack statusItem = createItem(
                Material.NETHER_STAR,
                ChatColor.GOLD + "" + ChatColor.BOLD + "Server Performance Status",
                ChatColor.GRAY + "Current TPS: " + tpsColor + String.format("%.2f", budget.getTps()),
                ChatColor.GRAY + "Current MSPT: " + ChatColor.AQUA + String.format("%.2fms", budget.getMspt()),
                ChatColor.GRAY + "Budget Multiplier: " + ChatColor.YELLOW + String.format("%.0f%%", budget.getBudgetMultiplier() * 100.0),
                ChatColor.GRAY + "Active Mode: " + ChatColor.WHITE + plugin.getCompatibilityService().getMode().name(),
                ChatColor.GRAY + "Tracked Hibernations: " + ChatColor.GREEN + plugin.getLazySimService().getTrackedHibernatingCount()
        );
        inv.setItem(4, statusItem);

        // Slot 19: MobCap Service
        boolean mobCapOn = plugin.getMobCapService().isEnabled();
        inv.setItem(19, createItem(
                mobCapOn ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                (mobCapOn ? ChatColor.GREEN : ChatColor.RED) + "MobCap Service: " + (mobCapOn ? "ENABLED" : "DISABLED"),
                ChatColor.GRAY + "Dynamic load-budget entity scaling.",
                ChatColor.YELLOW + "Click to toggle"
        ));

        // Slot 21: Redstone Watchdog
        boolean redstoneOn = plugin.getRedstoneWatchdog().isEnabled();
        inv.setItem(21, createItem(
                redstoneOn ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                (redstoneOn ? ChatColor.GREEN : ChatColor.RED) + "Redstone Watchdog: " + (redstoneOn ? "ENABLED" : "DISABLED"),
                ChatColor.GRAY + "Signature loop and clock throttling.",
                ChatColor.YELLOW + "Click to toggle"
        ));

        // Slot 23: Chunk / Elytra Throttle
        boolean chunkOn = plugin.getChunkThrottleService().isEnabled();
        inv.setItem(23, createItem(
                chunkOn ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                (chunkOn ? ChatColor.GREEN : ChatColor.RED) + "Elytra & Chunk Throttle: " + (chunkOn ? "ENABLED" : "DISABLED"),
                ChatColor.GRAY + "Fair-share CPU budget and pre-generation.",
                ChatColor.YELLOW + "Click to toggle"
        ));

        // Slot 25: Lazy Simulation Engine
        boolean lazyOn = plugin.getLazySimService().isEnabled();
        inv.setItem(25, createItem(
                lazyOn ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                (lazyOn ? ChatColor.GREEN : ChatColor.RED) + "Lazy Simulation: " + (lazyOn ? "ENABLED" : "DISABLED"),
                ChatColor.GRAY + "Delta catch-up for unloaded chunks.",
                ChatColor.YELLOW + "Click to toggle"
        ));

        // Slot 31: Top Lag Source Viewer
        List<LagSource> lagSources = plugin.getRedstoneWatchdog().getTopLagSources(3);
        List<String> lagLore = new ArrayList<>();
        lagLore.add(ChatColor.GRAY + "Heaviest current chunk regions:");
        if (lagSources.isEmpty()) {
            lagLore.add(ChatColor.DARK_GRAY + "  (No severe lag sources detected)");
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
        inv.setItem(31, createItem(Material.COMPASS, ChatColor.GOLD + "Top Lag Sources", lagLore.toArray(new String[0])));

        // Slot 49: Reload Config
        inv.setItem(49, createItem(Material.BLAZE_POWDER, ChatColor.YELLOW + "" + ChatColor.BOLD + "Hot Reload Config",
                ChatColor.GRAY + "Reloads config.yml without restart.",
                ChatColor.YELLOW + "Click to reload"
        ));

        player.openInventory(inv);
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot == 19) {
            if (plugin.getMobCapService().isEnabled()) {
                plugin.getMobCapService().disable();
            } else {
                plugin.getMobCapService().enable();
            }
            open(player);
        } else if (slot == 21) {
            if (plugin.getRedstoneWatchdog().isEnabled()) {
                plugin.getRedstoneWatchdog().disable();
            } else {
                plugin.getRedstoneWatchdog().enable();
            }
            open(player);
        } else if (slot == 23) {
            if (plugin.getChunkThrottleService().isEnabled()) {
                plugin.getChunkThrottleService().disable();
            } else {
                plugin.getChunkThrottleService().enable();
            }
            open(player);
        } else if (slot == 25) {
            if (plugin.getLazySimService().isEnabled()) {
                plugin.getLazySimService().disable();
            } else {
                plugin.getLazySimService().enable();
            }
            open(player);
        } else if (slot == 49) {
            plugin.reloadConfig();
            plugin.getMobCapService().reload();
            plugin.getRedstoneWatchdog().reload();
            plugin.getCompatibilityService().reload();
            player.sendMessage(ChatColor.GREEN + "[ASC] Adaptive Server Core reloaded successfully!");
            open(player);
        }
    }
}
