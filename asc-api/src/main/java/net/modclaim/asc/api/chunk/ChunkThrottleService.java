package net.modclaim.asc.api.chunk;

import net.modclaim.asc.api.ASCService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Service managing fair-share CPU budgets for chunk generation and elytra flight.
 */
public interface ChunkThrottleService extends ASCService {

    /**
     * Records player flight activity and updates trajectory calculations.
     *
     * @param player player to track
     */
    void trackPlayerMovement(@NotNull Player player);

    /**
     * Retrieves the flight profile of a player if available.
     *
     * @param playerUuid UUID of player
     * @return optional flight profile
     */
    @NotNull
    Optional<PlayerFlightProfile> getFlightProfile(@NotNull UUID playerUuid);

    /**
     * Determines whether a chunk generation task should proceed or be queued.
     *
     * @param player player triggering generation
     * @param chunkX target chunk X
     * @param chunkZ target chunk Z
     * @param priority assigned priority
     * @return true if permitted immediately, false if queued/throttled
     */
    boolean requestChunkGeneration(@NotNull Player player, int chunkX, int chunkZ, @NotNull ChunkPriority priority);

    /**
     * Adapts view-distance and simulation-distance for a player based on server load.
     *
     * @param player player to adjust
     * @param targetViewDistance calculated view distance
     * @param targetSimulationDistance calculated simulation distance
     */
    void adaptPlayerDistances(@NotNull Player player, int targetViewDistance, int targetSimulationDistance);

    /**
     * Checks whether dynamic distance adaptation is supported on the current platform.
     *
     * @return true if per-player distance adaptation is active
     */
    boolean isPerPlayerDistanceSupported();
}
