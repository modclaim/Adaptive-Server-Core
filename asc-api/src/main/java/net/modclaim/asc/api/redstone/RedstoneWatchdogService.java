package net.modclaim.asc.api.redstone;

import net.modclaim.asc.api.ASCService;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Service monitoring redstone update storms, clock loops, and hopper transfer overloads.
 */
public interface RedstoneWatchdogService extends ASCService {

    /**
     * Records a redstone update for a block and returns the prescribed throttle action.
     *
     * @param block the block updating
     * @param oldCurrent previous current level
     * @param newCurrent proposed new current level
     * @return prescribed throttle action
     */
    @NotNull
    ThrottleAction processBlockUpdate(@NotNull Block block, int oldCurrent, int newCurrent);

    /**
     * Checks if a hopper transfer in the target chunk should be permitted or throttled.
     *
     * @param sourceLocation location of source hopper/inventory
     * @return true if permitted, false if throttled
     */
    boolean canTransferItem(@NotNull Location sourceLocation);

    /**
     * Checks if a chunk or region is in the exempt whitelist.
     *
     * @param chunk target chunk
     * @return true if whitelisted
     */
    boolean isWhitelisted(@NotNull Chunk chunk);

    /**
     * Adds a chunk to the temporary or persistent whitelist.
     *
     * @param worldName world name
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     */
    void addWhitelist(@NotNull String worldName, int chunkX, int chunkZ);

    /**
     * Removes a chunk from the whitelist.
     *
     * @param worldName world name
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     */
    void removeWhitelist(@NotNull String worldName, int chunkX, int chunkZ);

    /**
     * Gets the top N heaviest lag sources currently detected.
     *
     * @param limit maximum results to return
     * @return sorted list of lag sources
     */
    @NotNull
    List<LagSource> getTopLagSources(int limit);

    /**
     * Retrieves an identified signature for a location if one exists.
     *
     * @param location target location
     * @return optional redstone signature
     */
    @NotNull
    Optional<RedstoneSignature> getSignatureAt(@NotNull Location location);
}
