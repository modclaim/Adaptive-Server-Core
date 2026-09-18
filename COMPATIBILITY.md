# Adaptive Server Core (ASC) — Version & Platform Compatibility Matrix

The following matrix details platform support tiers, native hooks, and functional constraints across Minecraft server versions and server software:

| MC Version | Platform | Support Tier | Key Features & Native Hooks | Constraints |
| :--- | :--- | :--- | :--- | :--- |
| **1.20.x – 1.21+** | **Folia** | **Tier 1 (Full Native)** | RegionScheduler & GlobalRegionScheduler, Thread-per-region safety, Nanosecond MSPT metrics, Per-player view/sim distance | Folia architecture requires global calls to route through regional dispatchers |
| **1.20.x – 1.21+** | **Paper / Purpur** | **Tier 1 (Full Native)** | ServerTickEndEvent, Async chunk pre-generation (`getChunkAtAsync`), Per-player view distance, Block physics hooks | None |
| **1.16.x – 1.19.x** | **Paper Legacy** | **Tier 2 (High Compatibility)** | Paper legacy scheduler, Async chunk loading, Load-Budget mob cap, Complete LazySim engine | Per-player simulation distance requires MC 1.18+ |
| **1.18.x – 1.21+** | **Spigot** | **Tier 2 (Standard Spigot)** | Standard Bukkit events, Global view-distance adaptation, LazySim delta catch-up, Watchdog | Per-player independent view distance not available (global adaptation used), native async chunk priority limited |
| **1.16.x – 1.17.x** | **Spigot / Bukkit** | **Tier 3 (Graceful Fallback)** | Core Bukkit API, Mob cap budget scaling, Redstone/Hopper loop throttling, LazySim baseline formulas | View distance fixed via `server.properties`, modern block types use fallback equivalents |

---

## External Plugin Integration Matrix

| Plugin | Detection Method | Action Taken |
| :--- | :--- | :--- |
| **WorldGuard** | Soft-depend | Hooks region boundaries for per-region mob caps and trusted technical zones |
| **Chunky** | Soft-depend | Temporarily relaxes chunk generation throttling during active world pre-generation jobs |
| **ClearLag** | Conflict Detection | ASC automatically switches to `COMPATIBILITY_REDUCED` mode and disables duplicate entity sweeper tasks |
| **LaggRemover** | Conflict Detection | Conflicting memory cleaners are detected and ASC operates in safe compatibility mode |
| **MythicMobs** | Soft-depend | Custom RPG bosses and configured models are strictly exempted from despawn routines |
| **PlaceholderAPI** | Soft-depend | Registers `%asc_tps%`, `%asc_mspt%`, `%asc_budget%`, and `%asc_hibernating_chunks%` placeholders |
| **Vault** | Soft-depend | Permission and economy system integration |
| **ProtocolLib** | Soft-depend | Packet-level movement and network monitoring hooks |
