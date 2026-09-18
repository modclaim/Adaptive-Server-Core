package net.modclaim.asc.api.lazysim;

import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

/**
 * Extensible interface implemented by subsystem processors (furnaces, crops, villagers, etc.)
 * or third-party plugins to execute light-weight delta catch-up math upon chunk load.
 */
public interface LazySimulatable {

    /**
     * Unique identifier of this processor.
     *
     * @return processor id
     */
    @NotNull
    String getId();

    /**
     * Execution order priority (lower numbers run earlier).
     *
     * @return priority order
     */
    int getPriority();

    /**
     * Invoked when an unloaded chunk reawakens.
     *
     * @param context catch-up context
     * @return simulation result metrics
     */
    @NotNull
    CatchUpResult onCatchUp(@NotNull CatchUpContext context);

    /**
     * Invoked when a chunk is unloaded and enters hibernation.
     *
     * @param chunk hibernating chunk
     * @param epochMs timestamp of hibernation
     */
    void onHibernate(@NotNull Chunk chunk, long epochMs);
}
