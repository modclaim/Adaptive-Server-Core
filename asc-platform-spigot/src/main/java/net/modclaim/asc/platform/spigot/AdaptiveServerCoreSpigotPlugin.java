package net.modclaim.asc.platform.spigot;

import net.modclaim.asc.api.ASCPlugin;
import net.modclaim.asc.api.chunk.ChunkThrottleService;
import net.modclaim.asc.api.compatibility.CompatibilityService;
import net.modclaim.asc.api.lazysim.LazySimService;
import net.modclaim.asc.api.mobcap.MobCapService;
import net.modclaim.asc.api.redstone.RedstoneWatchdogService;
import net.modclaim.asc.core.ASCPluginController;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdaptiveServerCoreSpigotPlugin extends JavaPlugin implements ASCPlugin {

    private ASCPluginController controller;

    @Override
    public void onEnable() {
        this.controller = new ASCPluginController(this, "Spigot");
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
        return controller.getPlatformName();
    }
}
