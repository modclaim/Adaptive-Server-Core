package net.modclaim.asc.api;

import net.modclaim.asc.api.chunk.ChunkThrottleService;
import net.modclaim.asc.api.compatibility.CompatibilityService;
import net.modclaim.asc.api.lazysim.LazySimService;
import net.modclaim.asc.api.mobcap.MobCapService;
import net.modclaim.asc.api.redstone.RedstoneWatchdogService;
import org.bukkit.plugin.Plugin;

/**
 * Primary interface for the Adaptive Server Core plugin instance.
 */
public interface ASCPlugin extends Plugin {

    /**
     * Retrieves the active Mob Cap management service.
     *
     * @return mob cap service
     */
    MobCapService getMobCapService();

    /**
     * Retrieves the active Redstone and Hopper watchdog service.
     *
     * @return redstone watchdog service
     */
    RedstoneWatchdogService getRedstoneWatchdog();

    /**
     * Retrieves the Elytra flight and chunk generation throttle service.
     *
     * @return chunk throttle service
     */
    ChunkThrottleService getChunkThrottleService();

    /**
     * Retrieves the Chunk Hibernation and Lazy Simulation service.
     *
     * @return lazy simulation service
     */
    LazySimService getLazySimService();

    /**
     * Retrieves the compatibility shim and integration service.
     *
     * @return compatibility service
     */
    CompatibilityService getCompatibilityService();

    /**
     * Retrieves the current core version string.
     *
     * @return version string
     */
    String getCoreVersion();

    /**
     * Retrieves the detected underlying platform brand name (e.g. Paper, Folia, Purpur, Spigot, Bukkit).
     *
     * @return platform name
     */
    String getPlatformName();
}
