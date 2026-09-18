package net.modclaim.asc.core.command;

import net.modclaim.asc.api.ASCPlugin;
import net.modclaim.asc.api.mobcap.LoadBudget;
import net.modclaim.asc.api.mobcap.MobCategory;
import net.modclaim.asc.api.redstone.LagSource;
import net.modclaim.asc.core.config.ProfileManager;
import net.modclaim.asc.core.gui.ASCAdminGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Main command executor for /asc commands.
 */
public final class ASCCommand implements CommandExecutor {

    private final ASCPlugin plugin;
    private final ProfileManager profileManager;
    private final ASCAdminGUI adminGui;

    public ASCCommand(@NotNull ASCPlugin plugin, @NotNull ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.adminGui = new ASCAdminGUI(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("asc.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use /asc.");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                adminGui.open(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help":
                sendHelp(sender);
                break;

            case "gui":
                if (sender instanceof Player player) {
                    adminGui.open(player);
                } else {
                    sender.sendMessage(ChatColor.RED + "The GUI can only be opened by in-game players.");
                }
                break;

            case "reload":
                plugin.reloadConfig();
                plugin.getMobCapService().reload();
                plugin.getRedstoneWatchdog().reload();
                plugin.getCompatibilityService().reload();
                sender.sendMessage(ChatColor.GREEN + "[ASC] Configuration and services reloaded successfully.");
                break;

            case "status":
                sendStatus(sender);
                break;

            case "diagnose":
                for (String line : plugin.getCompatibilityService().generateDiagnosticReport()) {
                    sender.sendMessage(line);
                }
                break;

            case "lagsources":
                int limit = 5;
                if (args.length > 1) {
                    try {
                        limit = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }
                sendLagSources(sender, limit);
                break;

            case "mobcap":
                handleMobCap(sender, args);
                break;

            case "profile":
                handleProfile(sender, args);
                break;

            case "lazysim":
                handleLazySim(sender, args);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /asc help for available commands.");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== [Adaptive Server Core (ASC) Commands] ===");
        sender.sendMessage(ChatColor.YELLOW + "/asc gui " + ChatColor.GRAY + "- Open the visual admin control panel");
        sender.sendMessage(ChatColor.YELLOW + "/asc status " + ChatColor.GRAY + "- Show real-time TPS, MSPT and load budget");
        sender.sendMessage(ChatColor.YELLOW + "/asc diagnose " + ChatColor.GRAY + "- Run compatibility diagnostic scan");
        sender.sendMessage(ChatColor.YELLOW + "/asc lagsources [limit] " + ChatColor.GRAY + "- Show heaviest chunks by MSPT impact");
        sender.sendMessage(ChatColor.YELLOW + "/asc mobcap get|monitor " + ChatColor.GRAY + "- Inspect or monitor dynamic mob caps");
        sender.sendMessage(ChatColor.YELLOW + "/asc profile save|load|list " + ChatColor.GRAY + "- Manage server performance profiles");
        sender.sendMessage(ChatColor.YELLOW + "/asc lazysim stats|purge " + ChatColor.GRAY + "- Chunk hibernation and catch-up controls");
        sender.sendMessage(ChatColor.YELLOW + "/asc reload " + ChatColor.GRAY + "- Hot-reload configuration without restart");
    }

    private void sendStatus(CommandSender sender) {
        LoadBudget budget = plugin.getMobCapService().getCurrentBudget();
        ChatColor tpsColor = budget.getTps() >= 19.0 ? ChatColor.GREEN : (budget.getTps() >= 15.0 ? ChatColor.YELLOW : ChatColor.RED);

        sender.sendMessage(ChatColor.GOLD + "=== [ASC Live Performance Status] ===");
        sender.sendMessage(ChatColor.GRAY + "TPS: " + tpsColor + String.format("%.2f", budget.getTps()) +
                ChatColor.GRAY + " | MSPT: " + ChatColor.AQUA + String.format("%.2fms", budget.getMspt()));
        sender.sendMessage(ChatColor.GRAY + "Load Budget: " + ChatColor.YELLOW + String.format("%.0f%%", budget.getBudgetMultiplier() * 100.0) +
                ChatColor.GRAY + " (Multiplier: " + String.format("%.2f", budget.getBudgetMultiplier()) + "x)");
        sender.sendMessage(ChatColor.GRAY + "Online Players: " + ChatColor.WHITE + budget.getOnlinePlayers());
        sender.sendMessage(ChatColor.GRAY + "Tracked Hibernations: " + ChatColor.GREEN + plugin.getLazySimService().getTrackedHibernatingCount());
        sender.sendMessage(ChatColor.GRAY + "Operating Mode: " + ChatColor.WHITE + plugin.getCompatibilityService().getMode().name());
    }

    private void sendLagSources(CommandSender sender, int limit) {
        List<LagSource> sources = plugin.getRedstoneWatchdog().getTopLagSources(limit);
        sender.sendMessage(ChatColor.GOLD + "=== [Top " + limit + " Server Lag Sources] ===");
        if (sources.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "No significant lag machines or heavy chunks detected.");
            return;
        }
        int rank = 1;
        for (LagSource s : sources) {
            sender.sendMessage(String.format(
                    "%s#%d. %s [%d, %d] %s~%.1fms %s| RS: %d/s, Hoppers: %d/s, Entities: %d %s",
                    ChatColor.YELLOW, rank++, s.getWorldName(), s.getChunkX(), s.getChunkZ(),
                    ChatColor.RED, s.getEstimatedMsptImpact(),
                    ChatColor.GRAY, s.getRedstoneUpdatesPerSec(), s.getHopperTransfersPerSec(), s.getEntityCount(),
                    s.isThrottled() ? ChatColor.LIGHT_PURPLE + "[THROTTLED]" : ""
            ));
        }
    }

    private void handleMobCap(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /asc mobcap <get|monitor>");
            return;
        }
        String action = args[1].toLowerCase();
        if (action.equals("monitor")) {
            if (sender instanceof Player player) {
                plugin.getMobCapService().toggleMonitor(player);
            } else {
                sender.sendMessage(ChatColor.RED + "Monitor HUD can only be toggled for in-game players.");
            }
        } else if (action.equals("get")) {
            LoadBudget b = plugin.getMobCapService().getCurrentBudget();
            sender.sendMessage(ChatColor.GOLD + "=== Current Dynamic Mob Caps (Budget: " + String.format("%.0f%%", b.getBudgetMultiplier() * 100.0) + ") ===");
            for (MobCategory cat : MobCategory.values()) {
                sender.sendMessage(ChatColor.GRAY + "• " + cat.name() + ": " + ChatColor.WHITE + b.getCap(cat) + " (Base: " + cat.getDefaultCap() + ")");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /asc mobcap <get|monitor>");
        }
    }

    private void handleProfile(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /asc profile <save|load|list> [name]");
            return;
        }
        String sub = args[1].toLowerCase();
        if (sub.equals("list")) {
            List<String> profiles = profileManager.listProfiles();
            sender.sendMessage(ChatColor.GOLD + "Available Profiles: " + ChatColor.WHITE + String.join(", ", profiles));
        } else if (sub.equals("save")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /asc profile save <name>");
                return;
            }
            boolean ok = profileManager.saveProfile(args[2]);
            if (ok) {
                sender.sendMessage(ChatColor.GREEN + "Profile '" + args[2] + "' saved successfully.");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to save profile.");
            }
        } else if (sub.equals("load")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /asc profile load <name>");
                return;
            }
            boolean ok = profileManager.loadProfile(args[2]);
            if (ok) {
                plugin.reloadConfig();
                plugin.getMobCapService().reload();
                plugin.getRedstoneWatchdog().reload();
                sender.sendMessage(ChatColor.GREEN + "Profile '" + args[2] + "' loaded and applied successfully.");
            } else {
                sender.sendMessage(ChatColor.RED + "Profile '" + args[2] + "' not found.");
            }
        }
    }

    private void handleLazySim(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /asc lazysim <stats|purge>");
            return;
        }
        String sub = args[1].toLowerCase();
        if (sub.equals("stats")) {
            int count = plugin.getLazySimService().getTrackedHibernatingCount();
            sender.sendMessage(ChatColor.GOLD + "Lazy Simulation Stats:");
            sender.sendMessage(ChatColor.GRAY + "Tracked Hibernating Chunks: " + ChatColor.GREEN + count);
            sender.sendMessage(ChatColor.GRAY + "Active Processors: " + ChatColor.WHITE + plugin.getLazySimService().getProcessors().size());
        } else if (sub.equals("purge")) {
            int purged = plugin.getLazySimService().purgeStaleRecords();
            sender.sendMessage(ChatColor.GREEN + "Purged " + purged + " stale chunk hibernation records.");
        }
    }
}
