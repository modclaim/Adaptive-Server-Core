# Adaptive Server Core (ASC)

> **Enterprise-Grade Real-Time Performance Core & Adaptive Optimization Engine for Minecraft Servers**  
> *Paper | Purpur | Folia | Spigot | Bukkit (1.16 – 1.21+)*

---

## Table of Contents
- [1. About ASC](#1-about-asc)
- [2. Key Features & Modules](#2-key-features--modules)
  - [2.1 Dynamic & Adaptive Mob Cap (Load-Budget Engine)](#21-dynamic--adaptive-mob-cap-load-budget-engine)
  - [2.2 Redstone & Hopper Watchdog (Anti-Lag-Machine)](#22-redstone--hopper-watchdog-anti-lag-machine)
  - [2.3 Elytra & Chunk Generation Controller](#23-elytra--chunk-generation-controller)
  - [2.4 Chunk Hibernation & Lazy Simulation (`asc-lazysim`)](#24-chunk-hibernation--lazy-simulation-asc-lazysim)
  - [2.5 Compatibility Shims & External Plugin Integrations](#25-compatibility-shims--external-plugin-integrations)
- [3. Installation](#3-installation)
- [4. Commands & Permissions](#4-commands--permissions)
- [5. In-Game Control Panel (GUI)](#5-in-game-control-panel-gui)
- [6. Developer API](#6-developer-api)
- [7. Building from Source](#7-building-from-source)
- [8. Compatibility Matrix](#8-compatibility-matrix)

---

## 1. About ASC

**Adaptive Server Core (ASC)** is an intelligent, high-performance optimization core engineered specifically for large open-world RPG, technical, and faction Minecraft servers (e.g. 8K×4K custom maps, hundreds of NPCs, complex redstone systems, and player-driven economies).

Traditional anti-lag plugins resort to crude, destructive measures (hard mob-cap cutoffs, abrupt redstone breaking, clearing named entities, or visual stutter). **ASC works differently**:
- **Under low player counts or ample server headroom** (MSPT < 35ms), mob spawn limits smoothly scale up (up to 150%) so the world feels alive and engaging.
- **Under heavy server stress**, limits gracefully decay using smooth mathematical PID/gradient curves rather than harsh hard cutoffs.
- Automated farms and redstone circuits cannot stall or crash the server, yet remain functional through intelligent pulse throttling.
- Inactive chunks safely hibernate without halting the world: furnaces smelt, crops grow, and villagers restock their trades upon chunk reawakening with zero runtime tick cost!

---

## 2. Key Features & Modules

### 2.1 Dynamic & Adaptive Mob Cap (Load-Budget Engine)
- **Mathematical Load-Budget Model**: Entity spawns are regulated against rolling MSPT and TPS metrics.
- **Smooth Gradient Scaling**:
  $$\text{Budget}_{\text{effective}} = \text{Budget}_{\text{base}} \times \left(\frac{\text{TargetMSPT}}{\max(\text{TargetMSPT}, \text{CurrentMSPT})}\right)^{1.8} \times \text{Clamp}\left(\frac{\text{TPS} - 10}{10}, 0, 1\right)$$
- **Strict Despawn Whitelist**: Custom-named entities (`CustomName`), tamed animals (`Tameable`), leashed mobs (`Leashed`), persistent entities, and entities with passengers or vehicles are strictly protected and never despawned.
- **Admin Live HUD**: Toggled via `/asc mobcap monitor`, rendering real-time TPS, MSPT, budget percentage, and total entity counts on BossBar and Action Bar.

### 2.2 Redstone & Hopper Watchdog (Anti-Lag-Machine)
- **Signature-Based Loop Detection**: Identifies rapid oscillating clock circuits (e.g. piston loops, comparator clocks) within localized 3x3x3 volumes.
- **Non-Destructive Throttling**: Rather than breaking redstone mechanisms, ASC artificially throttles transmission frequency (`ThrottleAction.THROTTLE_PULSE`), allowing machines to remain functional at safe speeds.
- **Hopper Rate Limiter**: Tracks item transfers per chunk, enforcing rate limits (default 50 transfers/second) to prevent hopper spam lag machines.
- `/asc lagsources`: Displays the top-N heaviest chunks with precise coordinates, update rates, and estimated MSPT impact.

### 2.3 Elytra & Chunk Generation Controller
- **High-Speed Flight Detection**: Tracks players gliding with Elytra at velocities $v > 0.8$ blocks/tick and computes their trajectory vector $(\Delta x, \Delta z)$.
- **Predictive Pre-Generation**: Asynchronously pre-generates ahead-of-time candidate chunks during low MSPT periods so players do not experience border lag.
- **Dynamic View & Simulation Distance**: Automatically scales view and simulation distance smoothly based on MSPT load ($\pm 1$ chunk every 10 seconds to prevent pop-in artifacts).

### 2.4 Chunk Hibernation & Lazy Simulation (`asc-lazysim`)
The heaviest burden on Minecraft servers is keeping inactive chunks loaded in RAM. ASC allows chunks to unload freely, applying lightweight delta catch-up math upon reawakening:
1. **Furnaces, Smokers, and Blast Furnaces**: Calculates fuel consumption and items smelted over elapsed time, updating inventories without overflowing output stacks.
2. **Agriculture & Crops**: Simulates growth stages for wheat, carrots, potatoes, beetroot, and sugar cane using deterministic hydration and lighting formulas.
3. **Villager Trading Restocks**: Restores trade uses based on elapsed Minecraft day cycles ($1\text{ day} = 1200\text{ seconds}$).
4. **Animal Maturation & Breeding**: Advances baby animal ages to adulthood and resets breeding cooldowns.
5. **Beehives & Bee Nests**: Increments honey levels according to daylight hours.
6. **Extensible Plugin API**: Third-party plugins can register custom handlers using the `LazySimulatable` interface.

### 2.5 Compatibility Shims & External Plugin Integrations
- **WorldGuard**: Hooks region boundaries for per-region mob limits and trusted technical zones.
- **Chunky**: Relaxes chunk generation throttles while background pre-generation jobs are actively running.
- **ClearLag & LaggRemover**: Automatically switches to `COMPATIBILITY_REDUCED` mode, suppressing duplicate despawn tasks to prevent double-hook conflicts.
- **PlaceholderAPI**: Registers `%asc_tps%`, `%asc_mspt%`, `%asc_budget%`, `%asc_hibernating_chunks%`, and more.
- **MythicMobs**: Automatically whitelists custom RPG bosses and mobs from despawn passes.
- `/asc diagnose`: Generates a comprehensive diagnostic report of active shims and platform status.

---

## 3. Installation

ASC is distributed in two formats:
1. **Universal Jar (`AdaptiveServerCore-Universal.jar`)**: Single jar compatible with Folia, Paper, Purpur, Spigot, and Bukkit. Detects the host platform automatically at runtime.
2. **Platform-Specific Jars**: Dedicated jars (e.g. `AdaptiveServerCore-Paper-1.21.jar`) tailored specifically for modern Paper/Folia environments.

### Steps:
1. Place the JAR file into your server's `plugins/` directory.
2. Start the server or run `/asc reload`.
3. Configure your desired thresholds in `plugins/AdaptiveServerCore/config.yml`.

---

## 4. Commands & Permissions

All administrative commands require the `asc.admin` permission:

| Command | Description |
| :--- | :--- |
| `/asc gui` | Opens the 54-slot interactive chest control panel |
| `/asc status` | Displays real-time TPS, MSPT, load budget, and chunk states |
| `/asc diagnose` | Runs a compatibility diagnostic scan and reports active shims |
| `/asc lagsources [limit]` | Displays top server lag sources ranked by estimated MSPT impact |
| `/asc mobcap get` | Displays current dynamic mob caps for each category |
| `/asc mobcap monitor` | Toggles the real-time BossBar / Action Bar monitor HUD |
| `/asc profile save <name>` | Saves current runtime settings as a named profile |
| `/asc profile load <name>` | Loads and applies a saved performance profile (e.g. `event`, `low-spec`) |
| `/asc profile list` | Lists all available performance profiles |
| `/asc lazysim stats` | Displays stats on tracked hibernating chunks and processors |
| `/asc lazysim purge` | Prunes expired or stale hibernation records from SQLite |
| `/asc reload` | Hot-reloads all configuration files without a server restart |
| `/asc help` | Displays the command reference guide |

---

## 5. In-Game Control Panel (GUI)

The `/asc gui` command opens an interactive 54-slot chest interface:
- **Performance Beacon**: Displays live TPS, MSPT, and budget multiplier in lore.
- **Module Toggle Buttons**: Lime/Red concrete buttons to toggle MobCap, Redstone Watchdog, Elytra Throttle, and LazySim in real-time.
- **Top Lag Sources Compass**: Quick overview of the heaviest 3 chunk coordinates.
- **Hot-Reload Star**: One-click configuration reloading.

---

## 6. Developer API

External plugins can depend on the `asc-api` module via Maven or Gradle:

```kotlin
dependencies {
    compileOnly("net.modclaim.asc:asc-api:1.0.0-SNAPSHOT")
}
```

### Registering a Custom LazySim Processor:
```java
public class CustomCropProcessor implements LazySimulatable {
    @Override
    public String getId() { return "my_custom_crops"; }

    @Override
    public int getPriority() { return 15; }

    @Override
    public CatchUpResult onCatchUp(CatchUpContext context) {
        Chunk chunk = context.getChunk();
        Duration elapsed = context.getElapsed();
        // Mathematical delta catch-up calculation over elapsed time...
        return new CatchUpResult(getId(), 0, 5, 12000L, true, "Updated 5 custom blocks");
    }

    @Override
    public void onHibernate(Chunk chunk, long epochMs) {
        // Invoked when chunk enters hibernation...
    }
}

// Registration via global provider:
ASCProvider.get().getLazySimService().registerProcessor(new CustomCropProcessor());
```

---

## 7. Building from Source

Building ASC requires Java 21 and Gradle:

```bash
# Run automated tests across all modules:
./gradlew check test

# Package both Universal and platform-specific JARs:
./gradlew shadowJar
```

Packaged distribution JARs will be generated in:
- `asc-loader/build/libs/AdaptiveServerCore-Universal-1.0.0-SNAPSHOT.jar`
- `asc-platform-paper-latest/build/libs/AdaptiveServerCore-Paper-1.21-1.0.0-SNAPSHOT.jar`
- `asc-platform-paper-legacy/build/libs/AdaptiveServerCore-Paper-Legacy-1.0.0-SNAPSHOT.jar`
- `asc-platform-spigot/build/libs/AdaptiveServerCore-Spigot-1.0.0-SNAPSHOT.jar`
- `asc-platform-bukkit/build/libs/AdaptiveServerCore-Bukkit-1.0.0-SNAPSHOT.jar`

---

## 8. Compatibility Matrix

For detailed platform tiers and external plugin integrations, refer to [`COMPATIBILITY.md`](COMPATIBILITY.md).
