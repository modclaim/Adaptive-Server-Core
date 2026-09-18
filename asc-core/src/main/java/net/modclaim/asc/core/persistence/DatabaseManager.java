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

    public void recordChunkHibernation(@NotNull String world, int chunkX, int chunkZ, long epochMs) {
        String key = makeKey(world, chunkX, chunkZ);
        memoryCache.put(key, epochMs);

        if (!sqliteAvailable || connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO chunk_hibernation (world, chunk_x, chunk_z, last_simulated_epoch_ms) " +
                        "VALUES (?, ?, ?, ?) ON CONFLICT(world, chunk_x, chunk_z) DO UPDATE SET last_simulated_epoch_ms = excluded.last_simulated_epoch_ms")) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.setLong(4, epochMs);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving chunk hibernation to database", e);
        }
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
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error querying chunk hibernation record", e);
        }
        return Optional.empty();
    }

    public void removeChunkHibernation(@NotNull String world, int chunkX, int chunkZ) {
        String key = makeKey(world, chunkX, chunkZ);
        memoryCache.remove(key);

        if (!sqliteAvailable || connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM chunk_hibernation WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {
            ps.setString(1, world);
            ps.setInt(2, chunkX);
            ps.setInt(3, chunkZ);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error removing chunk hibernation record", e);
        }
    }

    public int purgeOldHibernationRecords(long olderThanEpochMs) {
        memoryCache.entrySet().removeIf(entry -> entry.getValue() < olderThanEpochMs);

        if (!sqliteAvailable || connection == null) return 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM chunk_hibernation WHERE last_simulated_epoch_ms < ?")) {
            ps.setLong(1, olderThanEpochMs);
            return ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error purging old hibernation records", e);
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
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
    }
}
