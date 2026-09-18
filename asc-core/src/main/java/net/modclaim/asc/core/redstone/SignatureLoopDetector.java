package net.modclaim.asc.core.redstone;

import net.modclaim.asc.api.redstone.RedstoneSignature;
import net.modclaim.asc.api.redstone.ThrottleAction;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Signature-based loop detector analyzing frequency and state oscillation patterns in redstone circuits.
 */
public final class SignatureLoopDetector {

    private static final int WINDOW_SIZE = 40;
    private static final int FREQUENCY_THRESHOLD_PER_SEC = 16;

    private static final class UpdateEntry {
        final long timestamp;
        final int x, y, z;
        final int oldCurrent, newCurrent;

        UpdateEntry(long timestamp, int x, int y, int z, int oldCurrent, int newCurrent) {
            this.timestamp = timestamp;
            this.x = x;
            this.y = y;
            this.z = z;
            this.oldCurrent = oldCurrent;
            this.newCurrent = newCurrent;
        }
    }

    private final Map<String, Deque<UpdateEntry>> chunkHistory = new ConcurrentHashMap<>();
    private final Map<String, RedstoneSignature> detectedSignatures = new ConcurrentHashMap<>();

    public ThrottleAction recordAndCheck(@NotNull Block block, int oldCurrent, int newCurrent) {
        Location loc = block.getLocation();
        String chunkKey = loc.getWorld().getName() + ":" + (loc.getBlockX() >> 4) + ":" + (loc.getBlockZ() >> 4);
        long now = System.currentTimeMillis();

        Deque<UpdateEntry> history = chunkHistory.computeIfAbsent(chunkKey, k -> new ArrayDeque<>(WINDOW_SIZE + 5));

        synchronized (history) {
            // Prune entries older than 1 second (1000ms)
            while (!history.isEmpty() && (now - history.peekFirst().timestamp) > 1000) {
                history.pollFirst();
            }

            history.addLast(new UpdateEntry(now, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), oldCurrent, newCurrent));

            // Count updates localized to this 3x3x3 volume
            int localizedCount = 0;
            int bx = loc.getBlockX();
            int by = loc.getBlockY();
            int bz = loc.getBlockZ();

            for (UpdateEntry entry : history) {
                if (Math.abs(entry.x - bx) <= 2 && Math.abs(entry.y - by) <= 2 && Math.abs(entry.z - bz) <= 2) {
                    localizedCount++;
                }
            }

            if (localizedCount >= FREQUENCY_THRESHOLD_PER_SEC) {
                long signatureHash = ((long) bx * 31 + by) * 31 + bz;
                RedstoneSignature signature = new RedstoneSignature(
                        chunkKey,
                        loc,
                        localizedCount,
                        signatureHash,
                        now - 1000,
                        now
                );
                detectedSignatures.put(chunkKey, signature);

                if (localizedCount >= (FREQUENCY_THRESHOLD_PER_SEC * 2)) {
                    return ThrottleAction.THROTTLE_PULSE;
                }
                return ThrottleAction.WARN;
            } else {
                detectedSignatures.remove(chunkKey);
            }
        }

        return ThrottleAction.NONE;
    }

    public Optional<RedstoneSignature> getSignatureForChunk(@NotNull String chunkKey) {
        return Optional.ofNullable(detectedSignatures.get(chunkKey));
    }

    public int getRecentUpdatesInChunk(@NotNull String chunkKey) {
        Deque<UpdateEntry> history = chunkHistory.get(chunkKey);
        if (history == null) return 0;
        synchronized (history) {
            return history.size();
        }
    }

    public void cleanupStaleRecords() {
        long now = System.currentTimeMillis();
        chunkHistory.entrySet().removeIf(entry -> {
            Deque<UpdateEntry> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && (now - deque.peekFirst().timestamp) > 3000) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
    }
}
