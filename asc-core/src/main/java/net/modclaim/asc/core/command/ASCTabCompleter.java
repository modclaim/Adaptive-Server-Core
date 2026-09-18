package net.modclaim.asc.core.command;

import net.modclaim.asc.core.config.ProfileManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Rich tab completer for /asc commands.
 */
public final class ASCTabCompleter implements TabCompleter {

    private final ProfileManager profileManager;
    private static final List<String> ROOT_SUBS = Arrays.asList(
            "gui", "status", "diagnose", "lagsources", "mobcap", "profile", "lazysim", "reload", "help"
    );

    public ASCTabCompleter(@NotNull ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("asc.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterMatching(args[0], ROOT_SUBS);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("mobcap")) {
                return filterMatching(args[1], Arrays.asList("get", "monitor"));
            }
            if (sub.equals("profile")) {
                return filterMatching(args[1], Arrays.asList("save", "load", "list"));
            }
            if (sub.equals("lazysim")) {
                return filterMatching(args[1], Arrays.asList("stats", "purge"));
            }
            if (sub.equals("lagsources")) {
                return filterMatching(args[1], Arrays.asList("5", "10", "20"));
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("profile") && args[1].equalsIgnoreCase("load")) {
            return filterMatching(args[2], profileManager.listProfiles());
        }

        return Collections.emptyList();
    }

    private static List<String> filterMatching(String input, List<String> candidates) {
        List<String> matches = new ArrayList<>();
        String lower = input.toLowerCase();
        for (String c : candidates) {
            if (c.toLowerCase().startsWith(lower)) {
                matches.add(c);
            }
        }
        return matches;
    }
}
