# Changelog — Adaptive Server Core (ASC)

All notable changes and releases for Adaptive Server Core are documented in this file.

---

## [1.0.1] — 2026-09-18

### 🐛 Critical Bug Fixes
- **Player Skin Layer Desync**:
  - Fixed a desynchronization bug where player skins would load only half-way (missing 3D jacket, sleeve, and pant layers) on Paper 1.20–1.21+ servers.
  - View distance and simulation distance adjustments are now strictly dispatched on the main server thread (`scheduler.runSync`), preserving clientbound entity tracker bitmasks.
  - Added hysteresis damping (requiring sustained high load for 3+ streaks or low load for 5+ streaks) to eliminate view distance flapping.

- **Elytra Gliding & Fast Player Scatter Chunk Stalls**:
  - Fixed severe lag and chunk loading freezes when flying at maximum speed with Elytra or when players scatter quickly into unloaded terrain.
  - Added a fast-exit check in `PlayerMoveEvent` for non-gliding players, eliminating unnecessary coordinate math during walking and running.
  - Rate-limited Elytra trajectory tracking to at most once every 500ms per player (2 checks/second instead of 30+).
  - Introduced an `inFlightChunks` deduplication registry to prevent spamming duplicate asynchronous chunk load requests to Paper's `ChunkTaskScheduler`.

- **Console Log Spam & Main Thread Micro-Freezes**:
  - Eliminated synchronous SQLite disk I/O on the main server tick thread during chunk hibernation updates.
  - Implemented a non-blocking in-memory write-behind buffer backed by a daemon background executor (`asyncFlusher`) committing batched writes every 5 seconds.
  - Completely resolved `SQLITE_BUSY` database lock warnings and 10–50ms main thread pauses under heavy chunk unload volume.

- **Natural World Generation Spawns**:
  - Excluded `CHUNK_GEN` spawn reason from mob cap throttling in `DefaultMobCapService`. Animals and ambient creatures generated with new chunks are now preserved without interruption.

### 🎨 In-Game Admin GUI Enhancements
- **Click Protection**: Implemented `ASCGuiHolder` across all platforms to prevent items from being picked up or moved into player inventories upon clicking.
- **Intuitive Minecraft Item Icons**:
  - MobCap Module: `SPAWNER`
  - Redstone Watchdog: `REPEATER`
  - Chunk & Elytra Throttle: `ELYTRA`
  - Lazy Simulation: `FURNACE`
- **Dedicated Status Blocks**: Added clear, individual Green Concrete (`LIME_CONCRETE`) and Red Concrete (`RED_CONCRETE`) toggle switch blocks directly underneath each module icon for effortless visual management.

---

## [1.0.0] — 2026-09-18

### Initial Release Features
- **Gradle Multi-Module Architecture**:
  - `asc-api`: Public API interfaces, Bukkit events (`MobCapAdjustEvent`, `RedstoneThrottleEvent`, `ChunkCatchUpEvent`, `HopperThrottleEvent`), and data models.
  - `asc-core`: Load-budget mathematics, SQLite WAL database, configuration and profiles management, in-game chest GUI.
  - `asc-lazysim`: Chunk hibernation and lazy simulation engine with time-delta calculations (Furnaces, Crops, Villagers, Animals, Beehives).
  - `asc-platform-paper-latest`: Native Paper 1.21+ and Folia regional scheduling support with per-player view distance.
  - `asc-platform-paper-legacy`: Paper 1.16–1.19 legacy adaptation.
  - `asc-platform-spigot`: Spigot standard fallback adapter.
  - `asc-platform-bukkit`: Minimal Bukkit baseline with graceful degradation.
  - `asc-loader`: Runtime platform auto-detection and universal distribution jar.

- **Dynamic Mob Cap & Load-Budget**:
  - Real-time TPS and MSPT gradient scaling using PID-style mathematical curves.
  - Up to 150% mob cap bonus during low player count or idle server headroom.
  - Strict despawn whitelist (named, tamed, leashed, persistent, and passenger entities are protected).
  - Admin Live HUD (Action Bar and BossBar real-time monitoring).

- **Redstone & Hopper Watchdog**:
  - Ring-buffer signature loop detector to identify oscillating clock circuits.
  - Non-destructive pulse throttling to maintain farm functionality at safe rates.
  - Per-chunk hopper transfer frequency limits (transfers per second).
  - `/asc lagsources` diagnostic command ranking heaviest chunks by estimated MSPT impact.

- **Elytra & Chunk Generation Controller**:
  - High-speed gliding trajectory calculation and velocity vector prediction.
  - Predictive asynchronous pre-generation during idle server cycles.
  - Dynamic per-player view and simulation distance scaling based on MSPT load.

- **Chunk Hibernation & Lazy Simulation**:
  - Persistent SQLite WAL timestamp storage with automatic stale record pruning.
  - Mathematical simulation of furnace smelting and fuel consumption without inventory overflow.
  - Deterministic crop growth progression factoring in soil hydration and light levels.
  - Villager trade restocking based on elapsed Minecraft day cycles.
  - Baby animal maturation and breeding cooldown reset.
  - Beehive honey level accumulation.
  - Open `LazySimulatable` API for third-party plugin integrations.

- **Compatibility Shims & Integrations**:
  - Conflict avoidance: switches to `COMPATIBILITY_REDUCED` mode when ClearLag or LaggRemover is present.
  - WorldGuard, Chunky, MythicMobs, and PlaceholderAPI integrations.
  - `/asc diagnose` comprehensive diagnostic report.

- **Management & In-Game GUI**:
  - 54-slot interactive chest control panel (`/asc gui`).
  - `/asc reload`, `/asc status`, and `/asc profile save|load|list`.
  - Contextual tab completion for all commands.
