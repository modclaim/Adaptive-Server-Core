# Adaptive Server Core (ASC)

> **Enterprise-Grade Real-Time Performance Core & Adaptive Optimization Engine for Minecraft Servers**  
> *Paper | Purpur | Folia | Spigot | Bukkit (1.16 – 1.21+)*

---

## Mundarija / Table of Contents
- [1. Loyiha haqida (About ASC)](#1-loyiha-haqida-about-asc)
- [2. Asosiy Modullar va Funksiyalar (Key Features)](#2-asosiy-modullar-va-funksiyalar-key-features)
  - [2.1 Dinamik & Adaptiv Mob Cap (Load-Budget Engine)](#21-dinamik--adaptiv-mob-cap-load-budget-engine)
  - [2.2 Redstone & Hopper Watchdog (Anti-Lag-Machine)](#22-redstone--hopper-watchdog-anti-lag-machine)
  - [2.3 Elytra & Chunk Generation Controller](#23-elytra--chunk-generation-controller)
  - [2.4 Chunk Hibernation & Lazy Simulation (`asc-lazysim`)](#24-chunk-hibernation--lazy-simulation-asc-lazysim)
  - [2.5 Moslashuv va Integratsiyalar (Compatibility Shims)](#25-moslashuv-va-integratsiyalar-compatibility-shims)
- [3. O'rnatish (Installation)](#3-ornatish-installation)
- [4. Buyruqlar va Huquqlar (Commands & Permissions)](#4-buyruqlar-va-huquqlar-commands--permissions)
- [5. In-Game Boshqaruv Menyusi (GUI)](#5-in-game-boshqaruv-menyusi-gui)
- [6. Dasturchilar uchun API (Developer API)](#6-dasturchilar-uchun-api-developer-api)
- [7. Kompilyatsiya va Build qilish (Building from Source)](#7-kompilyatsiya-va-build-qilish-building-from-source)
- [8. Versiyalar Muvofiqlik Matritsasi (Compatibility Matrix)](#8-versiyalar-muvofiqlik-matritsasi-compatibility-matrix)

---

## 1. Loyiha haqida (About ASC)

**Adaptive Server Core (ASC)** — katta open-world, RPG, texnik yoki fraksiyali Minecraft serverlar uchun maxsus yaratilgan aqlli yadrodur.

Oddiy anti-lag plaginlari dunyoni "kesib tashlaydi" (hard mob cap cutoffs, redstone o'chirib qo'yish, o'yin tajribasini buzish). **ASC esa boshqacha ishlaydi**:
- **Kam o'yinchida** yoki server bemalol ishlayotganda (MSPT < 35ms) mob limitlari avtomatik oshadi — dunyo jonli va realistik bo'ladi.
- **Yuqori yukda** limitlar silliq (gradient PID formulasi asosida) pasaytiriladi.
- Fermalar va redstone mexanizmlari serverni sindira olmaydi, lekin ular to'xtab ham qolmaydi — chastotasi xavfsiz darajagacha sekinlashtiriladi (throttled).
- Unload bo'lgan chunklarda hayot to'xtamaydi: pechlar pishadi, o'simliklar o'sadi, villagerlar savdo zahiralarini tiklaydi — lekin bu server tickiga 0ms ta'sir qiladi!

---

## 2. Asosiy Modullar va Funksiyalar (Key Features)

### 2.1 Dinamik & Adaptiv Mob Cap (Load-Budget Engine)
- **Matematik Load-Budget modeli**: Moblar qat'iy cheklov bilan emas, serverning real MSPT va TPS ko'rsatkichlariga qarab spawn qilinadi.
- **Silliq o'tish (Gradient Scaling)**:
  $$\text{Budget}_{\text{effective}} = \text{Budget}_{\text{base}} \times \left(\frac{\text{TargetMSPT}}{\max(\text{TargetMSPT}, \text{CurrentMSPT})}\right)^{1.8} \times \text{Clamp}\left(\frac{\text{TPS} - 10}{10}, 0, 1\right)$$
- **Qat'iy Despawn Whitelist**: Nomlangan moblar (`CustomName`), xonakilashtirilgan hayvonlar (`Tameable`), ipli hayvonlar (`Leashed`), persistent entity'lar va arava/qayiqdagi entity'lar HECH QACHON despawn qilinmaydi.
- **Admin Live HUD**: `/asc mobcap monitor` orqali Action Bar va BossBar'da real-vaqt TPS, MSPT, byudjet va entity'lar soni ko'rsatiladi.

### 2.2 Redstone & Hopper Watchdog (Anti-Lag-Machine)
- **Signature-Based Loop Detection**: Bir xil kichik zonada (3x3x3) tebranuvchi soat mexanizmlarini (masalan piston/komparator loop) aniqlaydi.
- **Mexanizmni buzmasdan sekinlashtirish**: Mexanizmni sindirmaydi, signallarni o'tkazish oralig'ini sun'iy kechiktiradi (`ThrottleAction.THROTTLE_PULSE`).
- **Hopper Rate Limiter**: 1 chunkda 1 soniyada 50 tadan ortiq item ko'chirilishini nazorat qiladi va fermani me'yorida ushlab turadi.
- `/asc lagsources`: Eng ko'p server resursini yeyayotgan top-N chunk koordinatalarini aniq ko'rsatadi.

### 2.3 Elytra & Chunk Generation Controller
- **Tez uchishni aniqlash**: Elytra bilan $v > 0.8$ blok/tick tezlikda uchayotgan o'yinchilarning harakat vektori $(\Delta x, \Delta z)$ hisoblanadi.
- **Predictive Pre-Generation**: Server yuklamasi past paytda o'yinchi yo'nalishidagi chunklar oldindan asinxron yuklanadi.
- **Dinamik View & Simulation Distance**: Server yuki oshganda o'yinchilarning ko'rish masofasi silliq pasaytiriladi (pop-in sezilmasligi uchun har 10 sekundda $\pm 1$ blok).

### 2.4 Chunk Hibernation & Lazy Simulation (`asc-lazysim`)
Serverdagi eng og'ir narsa — bu bo'sh chunklarni xotirada ushlab turish. ASC ularni unload qilishga imkon beradi, lekin ular qayta yuklanganda $\Delta t$ vaqt farqi bo'yicha bitta operatsiyada hamma narsani hisoblab beradi:
1. **Pechlar, Smokerlar va Blast Furnacelar**: Yonilg'i sarfi va xomashyoni pishirish jarayoni aniq simulyatsiya qilinadi, mahsulot slotiga qo'shiladi (overflow xavfisiz).
2. **Qishloq xo'jaligi (Ekinlar)**: Bug'doy, sabzi, kartoshka, lavlagi, qandqamish kabilar namlik va yorug'lik hisobga olingan holda stoxastik formula bo'yicha o'stiriladi.
3. **Villagerlar Savdosi**: O'tgan Minecraft kunlari ($1\text{ kun} = 1200\text{s}$) bo'yicha savdo restocklari tiklanadi.
4. **Hayvonlar**: Yosh hayvonlar ulg'ayadi, breeding cooldown tiklanadi.
5. **Asalari uyalari**: Asal darajasi (honey level) ko'payadi.
6. **Ochiq Kengaytiriladigan API**: Istalgan tashqi plagin `LazySimulatable` interfeysi orqali o'z tizimini ro'yxatdan o'tkazishi mumkin!

### 2.5 Moslashuv va Integratsiyalar (Compatibility Shims)
- **WorldGuard**: Hududlar bo'yicha mustaqil limitlar va texnik zonalar whitelisti.
- **Chunky**: World pre-gen ketayotganda chunk throttling avtomatik yumshatiladi.
- **ClearLag / LaggRemover**: Agar aniqlansa, ASC avtomatik `COMPATIBILITY_REDUCED` rejimiga o'tib, ikkilangan despawn harakatlarini to'xtatadi (double-hook bo'lmaydi).
- **PlaceholderAPI**: `%asc_tps%`, `%asc_mspt%`, `%asc_budget%`, `%asc_hibernating_chunks%` va boshqalar.
- **MythicMobs**: Custom RPG bosslar va modellar himoyalanadi.
- `/asc diagnose`: Server diagnostikasini to'liq ko'rsatib beradi.

---

## 3. O'rnatish (Installation)

ASC ikki xil shaklda taqdim etiladi:
1. **Universal Jar (`AdaptiveServerCore-Universal.jar`)**: Istalgan platformaga (Folia, Paper, Purpur, Spigot, Bukkit) tushadi va platformani runtime'da o'zi aniqlaydi.
2. **Maxsus Platform Jar**: Masalan `AdaptiveServerCore-Paper-1.21.jar` faqat zamonaviy Paper/Folia uchun.

O'rnatish bosqichlari:
1. Jar faylni serveringizning `plugins/` papkasiga tashlang.
2. Serverni ishga tushiring yoki `/asc reload` qiling.
3. `plugins/AdaptiveServerCore/config.yml` faylida xohlagan sozlamalaringizni o'zgartiring.

---

## 4. Buyruqlar va Huquqlar (Commands & Permissions)

Barcha buyruqlar `asc.admin` huquqini talab qiladi:

| Buyruq | Tavsifi |
| :--- | :--- |
| `/asc gui` | Interaktiv 54-slotli boshqaruv menyusini ochish |
| `/asc status` | Real-vaqt TPS, MSPT, byudjet va chunklar holatini ko'rish |
| `/asc diagnose` | Plaginlar integratsiyasi va diagnostika hisoboti |
| `/asc lagsources [limit]` | Serverni eng ko'p sekinlashtirayotgan top koordinatalar |
| `/asc mobcap get` | Har bir kategoriya bo'yicha joriy dinamik limitlarni ko'rish |
| `/asc mobcap monitor` | BossBar / Action Bar live HUD'ni yoqish/o'chirish |
| `/asc profile save <nom>` | Joriy sozlamalarni yangi profil sifatida saqlash |
| `/asc profile load <nom>` | Saqlangan profilni serverga qo'llash (masalan `event`, `low-spec`) |
| `/asc profile list` | Mavjud profillar ro'yxatini ko'rish |
| `/asc lazysim stats` | Hibernatsiyadagi chunklar statistikasini ko'rish |
| `/asc lazysim purge` | Muddati o'tgan eski yozuvlarni tozalash |
| `/asc reload` | Konfiguratsiyani server restartisiz yangilash |
| `/asc help` | Barcha buyruqlar bo'yicha yordam |

---

## 5. In-Game Boshqaruv Menyusi (GUI)

`/asc gui` buyrug'i orqali ochiladigan menyuda:
- Real-vaqt server tezligi (Nether Star indikatori).
- **Modullarni yoqish/o'chirish tugmalari**: Yashil/Qizil bloklar orqali MobCap, Redstone Watchdog, Elytra Throttle va LazySim tizimlarini jonli yoqib-o'chirish.
- **Top Lag Manbalari kompassi**: Eng og'ir 3 ta chunk haqida qisqacha ma'lumot.
- **Hot-Reload tugmasi**: Bitta klik bilan konfiguratsiyani yangilash.

---

## 6. Dasturchilar uchun API (Developer API)

Tashqi plaginlar uchun Maven / Gradle orqali `asc-api` moduli ulanadi:

```kotlin
dependencies {
    compileOnly("net.modclaim.asc:asc-api:1.0.0-SNAPSHOT")
}
```

### Custom LazySim Processor qo'shish:
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
        // O'tgan vaqt (elapsed) bo'yicha matematik hisob-kitob...
        return new CatchUpResult(getId(), 0, 5, 12000L, true, "Updated 5 blocks");
    }

    @Override
    public void onHibernate(Chunk chunk, long epochMs) {
        // Chunk uxlashga ketgandagi amallar...
    }
}

// Ro'yxatdan o'tkazish:
ASCProvider.get().getLazySimService().registerProcessor(new CustomCropProcessor());
```

---

## 7. Kompilyatsiya va Build qilish (Building from Source)

Loyihani yig'ish uchun Java 21 va Gradle talab qilinadi:

```bash
# Barcha modullarni kompilyatsiya qilish va testlarni yurgizish:
./gradlew check test

# Barcha jar fayllarni (Universal va Platform jarlarni) yig'ish:
./gradlew shadowJar
```

Tayyor jar fayllar:
- `asc-loader/build/libs/AdaptiveServerCore-Universal-1.0.0-SNAPSHOT.jar`
- `asc-platform-paper-latest/build/libs/AdaptiveServerCore-Paper-1.21-1.0.0-SNAPSHOT.jar`
- `asc-platform-paper-legacy/build/libs/AdaptiveServerCore-Paper-Legacy-1.0.0-SNAPSHOT.jar`
- `asc-platform-spigot/build/libs/AdaptiveServerCore-Spigot-1.0.0-SNAPSHOT.jar`
- `asc-platform-bukkit/build/libs/AdaptiveServerCore-Bukkit-1.0.0-SNAPSHOT.jar`

---

## 8. Versiyalar Muvofiqlik Matritsasi (Compatibility Matrix)

Batafsil ma'lumot uchun [`COMPATIBILITY.md`](COMPATIBILITY.md) fayliga qarang.
