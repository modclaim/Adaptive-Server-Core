package net.modclaim.asc.api.chunk;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Encapsulates movement speed and predictive generation vector for a flying player.
 */
public final class PlayerFlightProfile {
    private final UUID playerUuid;
    private final boolean isGliding;
    private final double horizontalSpeed;
    private final Vector velocityVector;
    private final int currentViewDistance;
    private final int currentSimulationDistance;
    private final long lastUpdateEpochMs;

    public PlayerFlightProfile(
            @NotNull UUID playerUuid,
            boolean isGliding,
            double horizontalSpeed,
            @NotNull Vector velocityVector,
            int currentViewDistance,
            int currentSimulationDistance,
            long lastUpdateEpochMs
    ) {
        this.playerUuid = playerUuid;
        this.isGliding = isGliding;
        this.horizontalSpeed = horizontalSpeed;
        this.velocityVector = velocityVector;
        this.currentViewDistance = currentViewDistance;
        this.currentSimulationDistance = currentSimulationDistance;
        this.lastUpdateEpochMs = lastUpdateEpochMs;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isGliding() {
        return isGliding;
    }

    public double getHorizontalSpeed() {
        return horizontalSpeed;
    }

    public Vector getVelocityVector() {
        return velocityVector;
    }

    public int getCurrentViewDistance() {
        return currentViewDistance;
    }

    public int getCurrentSimulationDistance() {
        return currentSimulationDistance;
    }

    public long getLastUpdateEpochMs() {
        return lastUpdateEpochMs;
    }
}
