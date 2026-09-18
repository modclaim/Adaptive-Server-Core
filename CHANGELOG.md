# Changelog — Adaptive Server Core (ASC)

Barcha o'zgarishlar va versiyalar ushbu hujjatda qayd etib boriladi.

---

## [1.0.0] — 2026-09-18

### Yangi Qo'shilgan Imkoniyatlar (Features)
- **Gradle Multi-Module Arxitekturasi**:
  - `asc-api`: Ochiq API interfeyslari, Bukkit eventlari (`MobCapAdjustEvent`, `RedstoneThrottleEvent`, `ChunkCatchUpEvent` va h.k.) hamda ma'lumotlar tuzilmalari.
  - `asc-core`: Load-budget matematikasi, SQLite WAL ma'lumotlar ombori, sozlamalar va profillar boshqaruvi, in-game chest GUI.
  - `asc-lazysim`: Unloaded chunklar uchun vaqt-delta matematikasi bilan ishlovchi lazy simulation dvigateli (Pechlar, Ekinlar, Villagerlar, Hayvonlar, Asalari uyalari).
  - `asc-platform-paper-latest`: Paper 1.21+ va Folia uchun native region scheduler va per-player view distance hooklari.
  - `asc-platform-paper-legacy`: Paper 1.16–1.19 legacy moslashuvi.
  - `asc-platform-spigot`: Spigot standart fallback adapteri.
  - `asc-platform-bukkit`: Minimal Bukkit baseline graceful degradation bilan.
  - `asc-loader`: Runtime platformani avtomatik aniqlovchi universal jar moduli.

- **Dinamik Mob Cap & Load-Budget**:
  - Real-vaqt TPS va MSPT asosida gradientli PID scaling modeli.
  - Kam o'yinchida jonli dunyo uchun 150% gacha mob cap ko'tarilishi.
  - Qat'iy despawn whitelist (Nomlangan, xonakilashtirilgan, ipli va persistent moblar himoyalangan).
  - Admin Live HUD (Action Bar va BossBar orqali real-vaqt monitoring).

- **Redstone & Hopper Watchdog**:
  - Ring buferli signature loop detector (tebranuvchi soat looplarini aniqlash).
  - Mexanizmni sindirmasdan sun'iy ravishda sekinlashtiruvchi pulse-throttling.
  - Chunk bo'yicha hopper transfer chastotasi limiti (transfers/second).
  - `/asc lagsources` koordinatalar bilan eng og'ir chunklar diagnostikasi.

- **Elytra & Chunk Generation Controller**:
  - Tez uchayotgan o'yinchilarning tezlik va yo'nalish vektorini bashorat qilish.
  - Past yuklama paytida oldindan yuklovchi predictive chunk pre-generation.
  - MSPT yukiga qarab o'yinchilarning ko'rish masofasini (view-distance) silliq moslash.

- **Chunk Hibernation & Lazy Simulation**:
  - SQLite WAL persistent timestamp tizimi.
  - Pechlarda pishirish va yonilg'i sarfini delta hisoblash (inventar sig'imidan toshmagan holda).
  - Ekinlar uchun stoxastik yorug'lik va namlikka asoslangan o'sish modeli.
  - Villager savdo restocklarini kunlik tsikllar bo'yicha tiklash.
  - Bolalarning ulg'ayishi va ko'payish vaqtlarini tiklash.
  - Asalari uyalarida asal darajasini oshirish.
  - Tashqi plaginlar uchun ochiq `LazySimulatable` API.

- **Integratsiyalar & Diagnostika**:
  - ClearLag va LaggRemover bilan konflikt bo'lmasligi uchun avtomatik `COMPATIBILITY_REDUCED` rejimi.
  - WorldGuard, Chunky, MythicMobs va PlaceholderAPI integratsiyalari.
  - `/asc diagnose` orqali to'liq server diagnostikasi.

- **Boshqaruv & In-Game GUI**:
  - 54-slotli interaktiv boshqaruv paneli (`/asc gui`).
  - `/asc reload`, `/asc status`, `/asc profile save|load|list`.
  - Tab-completion qo'llab-quvvatlashi.
