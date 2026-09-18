package net.modclaim.asc.core.chunk;

import net.modclaim.asc.core.scheduler.ServerSchedulerAdapter;
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

    private final ServerSchedulerAdapter scheduler;
    private int highLoadStreak = 0;
    private int lowLoadStreak = 0;

    public DynamicDistanceManager(int minViewDistance, int maxViewDistance, @NotNull ServerSchedulerAdapter scheduler) {
        this.minViewDistance = Math.max(6, minViewDistance);
        this.maxViewDistance = Math.min(32, maxViewDistance);
        this.currentTargetViewDistance = 10;
        this.scheduler = scheduler;
        detectSupport();
    }

    public DynamicDistanceManager(@NotNull ServerSchedulerAdapter scheduler) {
        this(6, 12, scheduler);
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

        // Stabilized hysteresis: require sustained load to prevent view distance flapping
        if (mspt > 46.0) {
            highLoadStreak++;
            lowLoadStreak = 0;
            if (highLoadStreak >= 3 && currentTargetViewDistance > minViewDistance) {
                currentTargetViewDistance--;
                highLoadStreak = 0;
                applyToAllPlayers();
            }
        } else if (mspt < 30.0) {
            lowLoadStreak++;
            highLoadStreak = 0;
            if (lowLoadStreak >= 5 && currentTargetViewDistance < maxViewDistance) {
                currentTargetViewDistance++;
                lowLoadStreak = 0;
                applyToAllPlayers();
            }
        } else {
            highLoadStreak = 0;
            lowLoadStreak = 0;
        }
    }

    private void applyToAllPlayers() {
        if (!perPlayerSupported) return;
        final int targetView = currentTargetViewDistance;
        final int targetSim = Math.max(minViewDistance, currentTargetViewDistance - 2);

        // MUST ALWAYS RUN ON MAIN THREAD to prevent chunk corruption and player skin layer desyncs
        scheduler.runSync(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player != null && player.isOnline()) {
                    adaptPlayer(player, targetView, targetSim);
                }
            }
        });
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
