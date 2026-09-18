package net.modclaim.asc.core.chunk;

import net.modclaim.asc.api.chunk.ChunkPriority;
import net.modclaim.asc.api.chunk.PlayerFlightProfile;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts ahead-of-time chunks based on player trajectory and queues async pre-generation when load allows.
 */
public final class PredictiveChunkQueue {

    public static final class PredictedChunk {
        public final int chunkX;
        public final int chunkZ;
        public final ChunkPriority priority;

        public PredictedChunk(int chunkX, int chunkZ, ChunkPriority priority) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.priority = priority;
        }
    }

    @NotNull
    public List<PredictedChunk> predictCandidateChunks(@NotNull Player player, @NotNull PlayerFlightProfile profile) {
        List<PredictedChunk> candidates = new ArrayList<>();
        Location loc = player.getLocation();
        Vector vel = profile.getVelocityVector();

        int currentCx = loc.getBlockX() >> 4;
        int currentCz = loc.getBlockZ() >> 4;

        double normalizedX = vel.getX() / Math.max(0.001, profile.getHorizontalSpeed());
        double normalizedZ = vel.getZ() / Math.max(0.001, profile.getHorizontalSpeed());

        // Ahead-of-time chunks at 2, 4, and 6 chunks forward
        int[] steps = {2, 4, 6};
        ChunkPriority[] priorities = {ChunkPriority.HIGH, ChunkPriority.NORMAL, ChunkPriority.PREDICTIVE_LOW};

        for (int i = 0; i < steps.length; i++) {
            int step = steps[i];
            int targetCx = currentCx + (int) Math.round(normalizedX * step);
            int targetCz = currentCz + (int) Math.round(normalizedZ * step);

            candidates.add(new PredictedChunk(targetCx, targetCz, priorities[i]));
        }

        return candidates;
    }
}
