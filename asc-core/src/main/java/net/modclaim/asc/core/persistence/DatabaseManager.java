package net.modclaim.asc.core.persistence;

import net.modclaim.asc.api.lazysim.HibernationTicket;
import net.modclaim.asc.api.redstone.LagSource;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages high-performance SQLite persistence with WAL mode and in-memory caching fallback.
 */
public final class DatabaseManager {

    private final Plugin plugin;
    private final File dbFile;
    private Connection connection;
    private boolean sqliteAvailable = false;

    // Fast in-memory cache of chunk hibernation timestamps: "world:x:z" -> epochMs
    private final Map<String, Long> memoryCache = new ConcurrentHashMap<>();

    public DatabaseManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.dbFile = new File(dataDir, "asc.db");
    }

    public synchronized void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            this.connection = DriverManager.getConnection(url);

            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode = WAL;");
                st.execute("PRAGMA synchronous = NORMAL;");
                st.execute("PRAGMA temp_store = MEMORY;");

                st.execute("CREATE TABLE IF NOT EXISTS chunk_hibernation (" +
                        "world VARCHAR(64) NOT NULL, " +
                        "chunk_x INT NOT NULL, " +
                        "chunk_z INT NOT NULL, " +
                        "last_simulated_epoch_ms BIGINT NOT NULL, " +
                        "PRIMARY KEY (world, chunk_x, chunk_z)" +
                        ");");

                st.execute("CREATE TABLE IF NOT EXISTS performance_profiles (" +
                        "profile_name VARCHAR(64) PRIMARY KEY, " +
                        "config_yaml TEXT NOT NULL, " +
                        "created_epoch_ms BIGINT NOT NULL" +
                        ");");

                st.execute("CREATE TABLE IF NOT EXISTS lag_sources_log (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "epoch_ms BIGINT NOT NULL, " +
                        "world VARCHAR(64) NOT NULL, " +
                        "chunk_x INT NOT NULL, " +
                        "chunk_z INT NOT NULL, " +
                        "updates_per_sec INT NOT NULL, " +
                        "mspt REAL NOT NULL" +
                        ");");
            }
            this.sqliteAvailable = true;
            plugin.getLogger().info("SQLite database initialized successfully at: " + dbFile.getName());
            loadCacheFromDatabase();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "SQLite driver or database initialization failed. Falling back to in-memory/flatfile storage.", t);
            this.sqliteAvailable = false;
        }
    }

    private void loadCacheFromDatabase() {
        if (!sqliteAvailable || connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT world, chunk_x, chunk_z, last_simulated_epoch_ms FROM chunk_hibernation")) {
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                String key = makeKey(rs.getString("world"), rs.getInt("chunk_x"), rs.getInt("chunk_z"));
                memoryCache.put(key, rs.getLong("last_simulated_epoch_ms"));
                count++;
            }
            plugin.getLogger().info("Loaded " + count + " chunk hibernation records into cache.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to preload chunk hibernation records", e);
        }
    }

    private static String makeKey(String world, int cx, int cz) {
        return world + ":" + cx + ":" + cz;
    }

    // Non-blocking write-behind buffers for zero main-thread disk I/O
    private final Map<String, Long> pendingWrites = new ConcurrentHashMap<>();
    private final Set<String> pendingDeletes = ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.ScheduledExecutorService asyncFlusher =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ASC-Database-Flusher");
                t.setDaemon(true);
                return t;
            });

    {
        // Periodic background flush every 5 seconds
        asyncFlusher.scheduleWithFixedDelay(this::flushPendingToDatabase, 5, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void recordChunkHibernation(@NotNull String world, int chunkX, int chunkZ, long epochMs) {
        String key = makeKey(world, chunkX, chunkZ);
        memoryCache.put(key, epochMs);
        pendingWrites.put(key, epochMs);
        pendingDeletes.remove(key);
    }

    @NotNull
    public Optional<HibernationTicket> getChunkHibernation(@NotNull String world, int chunkX, int chunkZ) {
        String key = makeKey(world, chunkX, chunkZ);
        Long cached = memoryCache.get(key);
        if (cached != null) {
            return Optional.of(new HibernationTicket(world, chunkX, chunkZ, cached));
        }

        if (!sqliteAvailable || connection == null) return Optional.empty();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT last_simulated_epoch_ms FROM chunk_hibernation WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long epoch = rs.getLong(1);
                memoryCache.put(key, epoch);
                return Optional.of(new HibernationTicket(world, chunkX, chunkZ, epoch));
            }
        } catch (SQLException ignored) {}
        return Optional.empty();
    }

    public void removeChunkHibernation(@NotNull String world, int chunkX, int chunkZ) {
        String key = makeKey(world, chunkX, chunkZ);
        memoryCache.remove(key);
        pendingWrites.remove(key);
        pendingDeletes.add(key);
    }

    private synchronized void flushPendingToDatabase() {
        if (!sqliteAvailable || connection == null) return;
        if (pendingWrites.isEmpty() && pendingDeletes.isEmpty()) return;

        Map<String, Long> writesToFlush = new HashMap<>(pendingWrites);
        Set<String> deletesToFlush = new HashSet<>(pendingDeletes);

        try {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            if (!writesToFlush.isEmpty()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO chunk_hibernation (world, chunk_x, chunk_z, last_simulated_epoch_ms) " +
                                "VALUES (?, ?, ?, ?) ON CONFLICT(world, chunk_x, chunk_z) DO UPDATE SET last_simulated_epoch_ms = excluded.last_simulated_epoch_ms")) {
                    for (Map.Entry<String, Long> entry : writesToFlush.entrySet()) {
                        String[] parts = entry.getKey().split(":");
                        if (parts.length == 3) {
                            ps.setString(1, parts[0]);
                            ps.setInt(2, Integer.parseInt(parts[1]));
                            ps.setInt(3, Integer.parseInt(parts[2]));
                            ps.setLong(4, entry.getValue());
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
            }

            if (!deletesToFlush.isEmpty()) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM chunk_hibernation WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
                    for (String key : deletesToFlush) {
                        String[] parts = key.split(":");
                        if (parts.length == 3) {
                            ps.setString(1, parts[0]);
                            ps.setInt(2, Integer.parseInt(parts[1]));
                            ps.setInt(3, Integer.parseInt(parts[2]));
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
            }

            connection.commit();
            connection.setAutoCommit(originalAutoCommit);

            // Remove successfully committed records from buffer
            for (String k : writesToFlush.keySet()) {
                pendingWrites.remove(k, writesToFlush.get(k));
            }
            pendingDeletes.removeAll(deletesToFlush);
        } catch (SQLException ignored) {
            // Silently retry on next scheduled interval without logging spam or freezing the server
        }
    }

    public int purgeOldHibernationRecords(long olderThanEpochMs) {
        memoryCache.entrySet().removeIf(entry -> entry.getValue() < olderThanEpochMs);

        if (!sqliteAvailable || connection == null) return 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM chunk_hibernation WHERE last_simulated_epoch_ms < ?")) {
            ps.setLong(1, olderThanEpochMs);
            return ps.executeUpdate();
        } catch (SQLException ignored) {
            return 0;
        }
    }

    public int getTrackedHibernatingCount() {
        return memoryCache.size();
    }

    public void saveProfile(@NotNull String profileName, @NotNull String yamlContent) {
        if (!sqliteAvailable || connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO performance_profiles (profile_name, config_yaml, created_epoch_ms) " +
                        "VALUES (?, ?, ?) ON CONFLICT(profile_name) DO UPDATE SET config_yaml = excluded.config_yaml")) {
            ps.setString(1, profileName);
            ps.setString(2, yamlContent);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving performance profile: " + profileName, e);
        }
    }

    @Nullable
    public String loadProfile(@NotNull String profileName) {
        if (!sqliteAvailable || connection == null) return null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT config_yaml FROM performance_profiles WHERE profile_name = ?")) {
            ps.setString(1, profileName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error loading performance profile: " + profileName, e);
        }
        return null;
    }

    @NotNull
    public List<String> listProfiles() {
        List<String> list = new ArrayList<>();
        if (!sqliteAvailable || connection == null) return list;
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT profile_name FROM performance_profiles ORDER BY profile_name ASC");
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error listing profiles", e);
        }
        return list;
    }

    public void close() {
        try {
            asyncFlusher.shutdown();
            flushPendingToDatabase();
        } catch (Throwable ignored) {}

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
    }
}
