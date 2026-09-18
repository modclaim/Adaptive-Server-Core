# Changelog — Adaptive Server Core (ASC)

All notable changes and releases for Adaptive Server Core are documented in this file.

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
