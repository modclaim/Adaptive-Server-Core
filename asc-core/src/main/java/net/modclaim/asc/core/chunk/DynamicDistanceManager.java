package net.modclaim.asc.core.chunk;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Dynamically adjusts per-player view and simulation distances based on server load to prevent stutter.
 */
public final class DynamicDistanceManager {

    private final int minViewDistance;
    private final int maxViewDistance;
    private int currentTargetViewDistance;
    private boolean perPlayerSupported = false;

    private Method setViewDistanceMethod;
    private Method setSimDistanceMethod;

    public DynamicDistanceManager(int minViewDistance, int maxViewDistance) {
        this.minViewDistance = Math.max(4, minViewDistance);
        this.maxViewDistance = Math.min(32, maxViewDistance);
        this.currentTargetViewDistance = 10;
        detectSupport();
    }

    public DynamicDistanceManager() {
        this(6, 12);
    }

    private void detectSupport() {
        try {
            this.setViewDistanceMethod = Player.class.getMethod("setViewDistance", int.class);
            this.setSimDistanceMethod = Player.class.getMethod("setSimulationDistance", int.class);
            this.perPlayerSupported = true;
        } catch (NoSuchMethodException e) {
            this.perPlayerSupported = false;
        }
    }

    public boolean isPerPlayerSupported() {
        return perPlayerSupported;
    }

    public void updateLoad(double mspt) {
        if (!perPlayerSupported) return;

        if (mspt > 45.0 && currentTargetViewDistance > minViewDistance) {
            currentTargetViewDistance--;
            applyToAllPlayers();
        } else if (mspt < 32.0 && currentTargetViewDistance < maxViewDistance) {
            currentTargetViewDistance++;
            applyToAllPlayers();
        }
    }

    private void applyToAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            adaptPlayer(player, currentTargetViewDistance, Math.max(minViewDistance, currentTargetViewDistance - 2));
        }
    }

    public void adaptPlayer(@NotNull Player player, int targetViewDist, int targetSimDist) {
        if (!perPlayerSupported) return;

        try {
            if (setViewDistanceMethod != null) {
                int current = player.getViewDistance();
                if (current != targetViewDist) {
                    setViewDistanceMethod.invoke(player, targetViewDist);
                }
            }
            if (setSimDistanceMethod != null) {
                int currentSim = player.getSimulationDistance();
                if (currentSim != targetSimDist) {
                    setSimDistanceMethod.invoke(player, targetSimDist);
                }
            }
        } catch (Exception ignored) {}
    }

    public int getCurrentTargetViewDistance() {
        return currentTargetViewDistance;
    }
}
