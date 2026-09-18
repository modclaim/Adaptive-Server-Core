package net.modclaim.asc.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Global singleton accessor for the Adaptive Server Core API.
 */
public final class ASCProvider {

    private static ASCPlugin instance;

    private ASCProvider() {}

    /**
     * Returns the registered ASCPlugin instance.
     *
     * @return active plugin instance
     * @throws IllegalStateException if the API is not yet registered
     */
    @NotNull
    public static ASCPlugin get() {
        if (instance == null) {
            throw new IllegalStateException("Adaptive Server Core API has not been initialized yet!");
        }
        return instance;
    }

    /**
     * Registers the singleton instance. Reserved for internal plugin bootstrap.
     *
     * @param plugin plugin instance to register
     */
    public static void register(@Nullable ASCPlugin plugin) {
        instance = plugin;
    }

    /**
     * Checks if the API is registered and available.
     *
     * @return true if available
     */
    public static boolean isAvailable() {
        return instance != null;
    }
}
