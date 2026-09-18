package net.modclaim.asc.core.config;

import net.modclaim.asc.core.persistence.DatabaseManager;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages saving, loading, and switching pre-packaged performance profiles.
 */
public final class ProfileManager {

    private final Plugin plugin;
    private final DatabaseManager databaseManager;
    private final File profilesFolder;

    public ProfileManager(@NotNull Plugin plugin, @NotNull DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.profilesFolder = new File(plugin.getDataFolder(), "profiles");
        if (!profilesFolder.exists()) {
            profilesFolder.mkdirs();
            createDefaultProfiles();
        }
    }

    private void createDefaultProfiles() {
        // Event day profile
        saveFileProfile("event",
                "mobcap:\n" +
                "  enabled: true\n" +
                "  target-mspt: 45.0\n" +
                "  max-bonus-multiplier: 1.8\n" +
                "redstone-watchdog:\n" +
                "  enabled: true\n" +
                "  frequency-threshold: 24\n" +
                "  hopper-transfer-limit: 80\n"
        );

        // Low spec mode
        saveFileProfile("low-spec",
                "mobcap:\n" +
                "  enabled: true\n" +
                "  target-mspt: 35.0\n" +
                "  max-bonus-multiplier: 1.1\n" +
                "  min-floor-multiplier: 0.15\n" +
                "redstone-watchdog:\n" +
                "  enabled: true\n" +
                "  frequency-threshold: 12\n" +
                "  hopper-transfer-limit: 30\n"
        );
    }

    private void saveFileProfile(String name, String content) {
        File file = new File(profilesFolder, name + ".yml");
        if (!file.exists()) {
            try {
                Files.writeString(file.toPath(), content);
            } catch (IOException ignored) {}
        }
    }

    public boolean saveProfile(@NotNull String name) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return false;

        try {
            String content = Files.readString(configFile.toPath());
            File target = new File(profilesFolder, name + ".yml");
            Files.writeString(target.toPath(), content);
            databaseManager.saveProfile(name, content);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save profile: " + name, e);
            return false;
        }
    }

    public boolean loadProfile(@NotNull String name) {
        String content = null;
        File target = new File(profilesFolder, name + ".yml");
        if (target.exists()) {
            try {
                content = Files.readString(target.toPath());
            } catch (IOException ignored) {}
        }

        if (content == null) {
            content = databaseManager.loadProfile(name);
        }

        if (content == null) {
            return false;
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            Files.writeString(configFile.toPath(), content);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load profile: " + name, e);
            return false;
        }
    }

    @NotNull
    public List<String> listProfiles() {
        List<String> list = new ArrayList<>();
        File[] files = profilesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                list.add(f.getName().replace(".yml", ""));
            }
        }
        for (String dbProfile : databaseManager.listProfiles()) {
            if (!list.contains(dbProfile)) {
                list.add(dbProfile);
            }
        }
        return list;
    }
}
