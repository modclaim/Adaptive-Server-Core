package net.modclaim.asc.core.chunk;

import net.modclaim.asc.api.chunk.PlayerFlightProfile;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks high-speed gliding players, calculating velocity vectors and heading trajectories.
 */
public final class ElytraFlightWatchdog {

    private final Map<UUID, PlayerFlightProfile> flightProfiles = new ConcurrentHashMap<>();

    public void updatePlayerFlight(@NotNull Player player) {
        if (!player.isGliding()) {
            flightProfiles.remove(player.getUniqueId());
            return;
        }

        Vector vel = player.getVelocity();
        double horizontalSpeed = Math.sqrt(vel.getX() * vel.getX() + vel.getZ() * vel.getZ());

        // Standard walking is ~0.2-0.3 blocks/tick. High speed elytra is >1.0 blocks/tick
        if (horizontalSpeed > 0.8) {
            int viewDist = 10;
            int simDist = 10;
            try {
                viewDist = player.getViewDistance();
                simDist = player.getSimulationDistance();
            } catch (Throwable ignored) {}

            PlayerFlightProfile profile = new PlayerFlightProfile(
                    player.getUniqueId(),
                    true,
                    horizontalSpeed,
                    vel.clone(),
                    viewDist,
                    simDist,
                    System.currentTimeMillis()
            );
            flightProfiles.put(player.getUniqueId(), profile);
        } else {
            flightProfiles.remove(player.getUniqueId());
        }
    }

    public Optional<PlayerFlightProfile> getProfile(@NotNull UUID uuid) {
        return Optional.ofNullable(flightProfiles.get(uuid));
    }

    public void removePlayer(@NotNull UUID uuid) {
        flightProfiles.remove(uuid);
    }
}
