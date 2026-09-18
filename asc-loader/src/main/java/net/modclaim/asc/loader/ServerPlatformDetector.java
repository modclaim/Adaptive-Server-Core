package net.modclaim.asc.loader;

public final class ServerPlatformDetector {

    public enum Platform {
        FOLIA("Folia (Threaded Regions)"),
        PURPUR("Purpur"),
        PAPER_LATEST("Paper 1.20+"),
        PAPER_LEGACY("Paper 1.16-1.19"),
        SPIGOT("Spigot"),
        BUKKIT("Bukkit Minimal");

        private final String friendlyName;

        Platform(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public String getFriendlyName() {
            return friendlyName;
        }
    }

    public static Platform detect() {
        if (classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            return Platform.FOLIA;
        }
        if (classExists("org.purpurmc.purpur.PurpurConfig")) {
            return Platform.PURPUR;
        }
        if (classExists("io.papermc.paper.configuration.PaperConfigurations")) {
            return Platform.PAPER_LATEST;
        }
        if (classExists("com.destroystokyo.paper.PaperConfig")) {
            return Platform.PAPER_LEGACY;
        }
        if (classExists("org.spigotmc.SpigotConfig")) {
            return Platform.SPIGOT;
        }
        return Platform.BUKKIT;
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
