package net.modclaim.asc.loader;

import net.modclaim.asc.api.ASCPlugin;
import net.modclaim.asc.api.chunk.ChunkThrottleService;
import net.modclaim.asc.api.compatibility.CompatibilityService;
import net.modclaim.asc.api.lazysim.LazySimService;
import net.modclaim.asc.api.mobcap.MobCapService;
import net.modclaim.asc.api.redstone.RedstoneWatchdogService;
import net.modclaim.asc.core.ASCPluginController;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdaptiveServerCoreUniversalPlugin extends JavaPlugin implements ASCPlugin {

    private ASCPluginController controller;
    private ServerPlatformDetector.Platform detectedPlatform;

    @Override
    public void onLoad() {
        this.detectedPlatform = ServerPlatformDetector.detect();
        getLogger().info("Detected Server Architecture: " + detectedPlatform.getFriendlyName());
    }

    @Override
    public void onEnable() {
        if (detectedPlatform == null) {
            detectedPlatform = ServerPlatformDetector.detect();
        }
        this.controller = new ASCPluginController(this, detectedPlatform.getFriendlyName());
        this.controller.enable();
    }

    @Override
    public void onDisable() {
        if (controller != null) {
            controller.disable();
        }
    }

    @Override
    public MobCapService getMobCapService() {
        return controller.getMobCapService();
    }

    @Override
    public RedstoneWatchdogService getRedstoneWatchdog() {
        return controller.getRedstoneWatchdog();
    }

    @Override
    public ChunkThrottleService getChunkThrottleService() {
        return controller.getChunkThrottleService();
    }

    @Override
    public LazySimService getLazySimService() {
        return controller.getLazySimService();
    }

    @Override
    public CompatibilityService getCompatibilityService() {
        return controller.getCompatibilityService();
    }

    @Override
    public String getCoreVersion() {
        return getDescription().getVersion();
    }

    @Override
    public String getPlatformName() {
        return detectedPlatform != null ? detectedPlatform.getFriendlyName() : "Universal";
    }
}
