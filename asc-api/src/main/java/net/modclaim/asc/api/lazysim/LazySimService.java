package net.modclaim.asc.api.lazysim;

import net.modclaim.asc.api.ASCService;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Central service managing chunk hibernation timestamps and lazy simulation catch-up passes.
 */
public interface LazySimService extends ASCService {

    /**
     * Registers a custom LazySimulatable processor.
     *
     * @param processor processor to register
     */
    void registerProcessor(@NotNull LazySimulatable processor);

    /**
     * Unregisters a previously registered processor.
     *
     * @param processorId processor identifier
     */
    void unregisterProcessor(@NotNull String processorId);

    /**
     * Retrieves all registered processors in priority order.
     *
     * @return collection of processors
     */
    @NotNull
    Collection<LazySimulatable> getProcessors();

    /**
     * Records hibernation timestamp when a chunk is unloaded.
     *
     * @param chunk unloaded chunk
     */
    void handleChunkUnload(@NotNull Chunk chunk);

    /**
     * Executes the delta catch-up pass when a chunk is reloaded.
     *
     * @param chunk reloaded chunk
     */
    void handleChunkLoad(@NotNull Chunk chunk);

    /**
     * Retrieves the stored hibernation ticket for a chunk if recorded.
     *
     * @param worldName world name
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @return optional hibernation ticket
     */
    @NotNull
    Optional<HibernationTicket> getTicket(@NotNull String worldName, int chunkX, int chunkZ);

    /**
     * Gets the total number of chunks currently tracked in hibernation state.
     *
     * @return tracked chunk count
     */
    int getTrackedHibernatingCount();

    /**
     * Purges expired or stale hibernation records older than configured max threshold.
     *
     * @return number of records pruned
     */
    int purgeStaleRecords();
}
