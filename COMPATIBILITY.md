# Adaptive Server Core (ASC) — Version & Platform Compatibility Matrix

Quyidagi jadvalda turli Minecraft versiyalari va server platformalari bo'yicha qo'llab-quvvatlanish darajasi, mavjud native hooklar va cheklovlar keltirilgan:

| MC Versiyasi | Platforma | Qo'llab-quvvatlanish Darajasi | Asosiy Xususiyatlar & Hooklar | Cheklovlar |
| :--- | :--- | :--- | :--- | :--- |
| **1.20.x – 1.21+** | **Folia** | **Tier 1 (To'liq Native)** | RegionScheduler & GlobalRegionScheduler, Thread-per-region xavfsizligi, Nanosekund MSPT, Per-player view/sim distance | Folia arxitekturasi tufayli ba'zi global Bukkit API chaqiruvlari avtomatik region schedulerga yo'naltiriladi |
| **1.20.x – 1.21+** | **Paper / Purpur** | **Tier 1 (To'liq Native)** | ServerTickEndEvent, Async chunk pre-generation (`getChunkAtAsync`), Per-player view distance, Blok fizika hooklari | Hech qanday cheklov yo'q |
| **1.16.x – 1.19.x** | **Paper Legacy** | **Tier 2 (Yuqori daraja)** | Paper legacy scheduler, Async chunk yuklash, Load-Budget mob cap, LazySim to'liq | Per-player simulation distance faqat 1.18+ da mavjud |
| **1.18.x – 1.21+** | **Spigot** | **Tier 2 (Standard Spigot)** | Standart Bukkit eventlari, Global view-distance adaptatsiyasi, LazySim delta catch-up, Watchdog | Per-player mustaqil view-distance yo'q (global moslashadi), Async chunk native prioritizatsiyasi cheklangan |
| **1.16.x – 1.17.x** | **Spigot / Bukkit** | **Tier 3 (Graceful Fallback)** | Asosiy Bukkit API, Mob cap boshqaruvi, Redstone/Hopper loop throttling, LazySim asosiy formulalari | Ko'rish masofasi faqat `server.properties` orqali, ba'zi zamonaviy blok turlari (masalan yangi ekinlar/mis) fallback rejimida |

---

## Tashqi Plaginlar Bilan Integratsiya Matritsasi

| Plagin | Aniqlash Usuli | Amalga Oshiriladigan Amal |
| :--- | :--- | :--- |
| **WorldGuard** | Soft-depend | Hududiy chegaralarni aniqlash, per-region mob cap va texnik whitelist zonalari |
| **Chunky** | Soft-depend | World pre-generation davrida chunk throttlingni vaqtinchalik yumshatish |
| **ClearLag** | Conflict Detection | ASC avtomatik `COMPATIBILITY_REDUCED` rejimiga o'tadi va ikkilangan despawn tozalovchilarni o'chiradi |
| **LaggRemover** | Conflict Detection | Konfliktli xotira tozalagichlar aniqlanib, ASC compatibility rejimiga o'tadi |
| **MythicMobs** | Soft-depend | Maxsus RPG bosslar va modellar avtomatik despawn himoyasiga olinadi |
| **PlaceholderAPI** | Soft-depend | `%asc_tps%`, `%asc_mspt%`, `%asc_budget%`, `%asc_hibernating_chunks%` pleysholderlari ro'yxatdan o'tkaziladi |
| **Vault** | Soft-depend | Administrator huquqlari va iqtisodiy tizim integratsiyasi |
| **ProtocolLib** | Soft-depend | Packet darajasidagi harakatlar monitoringi |
