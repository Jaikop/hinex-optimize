<p align="center"><img src="docs/assets/icon-512.png" width="128" alt="HinexOptimize icon"></p>

# HinexOptimize

Client-side optimization mod for Minecraft Java **26.2 – 26.4** on **Fabric** and **NeoForge**,
built for the new renderer (OpenGL and Vulkan). It removes work the game does for nothing and never
changes how the game plays.

[Русский](#русский) · [Benchmarks](docs/BENCHMARKS.md) · [Changelog](CHANGELOG.md)

## Features

### Smooth render distance (on by default)

Vanilla treats every render distance change as "everything changed": it throws away every built chunk
section, rebuilds the whole visible world and shows no terrain at all for a few frames. HinexOptimize
moves the already built sections into the new grid instead. Raising the distance only builds the new
ring of chunks, lowering it just drops the far ones, and the world stays on screen.

Measured on 26.4 (Vulkan, Intel Iris Xe), switching 12 → 16 → 8 → 12 chunks:

| | vanilla | HinexOptimize |
|---|---|---|
| longest frame | 140 ms | **58 ms** |
| frames over 50 ms | 10 | **1** |
| chunk sections rebuilt | 1464 | **639** |
| frames with no terrain | 160 | **0** |

### Dynamic render distance (off by default)

Lowers the *rendered* distance one chunk at a time while your FPS stays below a target, and raises it
again when there is headroom. Your setting and the distance requested from the server stay as you set
them, so chunks stay loaded and coming back is cheap. Pairs with smooth render distance, so each step
costs one ring of chunks instead of a full rebuild.

### Built-in benchmark

`/hinexbench run [seconds]` spins the camera where you stand and reports average FPS, 1% / 0.1% lows,
draw calls and the resource reload time. `/hinexbench rdswitch` measures render distance switches,
`/hinexbench stats` shows live per-frame counters. On Fabric the command needs Fabric API; the mod
itself does not.

## Versions

| Minecraft | Fabric | NeoForge |
|---|---|---|
| 26.2 | ✓ | ✓ |
| 26.3 | ✓ | ✓ |
| 26.4-snapshot-4 | ✓ | — (no NeoForge build for 26.4 yet) |

Requires Java 25. The mod only does something on the client; installing it on a server is harmless.

## Configuration

`config/hinexoptimize.properties` is created on first launch. Restart the game after editing it.

| key | default | |
|---|---|---|
| `mixin` | `true` | master switch |
| `mixin.bench` | `true` | counters for `/hinexbench` (negligible overhead) |
| `mixin.render.smoothview` | `true` | smooth render distance |
| `mixin.render.dynamicview` | `false` | dynamic render distance |
| `dynamicview.target_fps` | `60` | frame rate to keep (a lower FPS limit in your settings wins) |
| `dynamicview.min_distance` | `6` | never render fewer chunks than this |
| `dynamicview.max_reduction` | `8` | never go more than this many chunks below your setting |

## Compatibility

Not yet tested together with mods that replace the chunk renderer (Sodium, Iris, VulkanMod).
If you run into a problem, turn the feature off in the config and
[open an issue](https://github.com/Jaikop/hinex-optimize/issues) with your `latest.log`.

## Building

JDK 25 is needed; Gradle picks it up by itself (`gradle/gradle-daemon-jvm.properties`).

```
.\gradlew build
```

Jars for every version end up in `versions/<version>-<loader>/build/libs/`;
`.\gradlew buildAndCollect` copies them to `build/libs/<mod version>/`.

One code base serves all versions through [Stonecutter](https://stonecutter.kikugie.dev/): differences
are marked with `//? if ...` comments. Run the client with `.\gradlew :26.3-fabric:runClient`
(or `:26.3-neoforge:runClient`, `:26.4-fabric:runClient`, ...).

Unattended benchmark for comparing builds (creates a fixed-seed world, measures, quits):

```
powershell -ExecutionPolicy Bypass -File scripts\bench.ps1 -Node 26.4-fabric -Scenario rdswitch -Tag test -Repeat 3
powershell -ExecutionPolicy Bypass -File scripts\bench-summary.ps1 -Scenario rdswitch
```

## License

[LGPL-3.0-only](LICENSE).

---

## Русский

Клиентский мод-оптимизация для Minecraft Java **26.2 – 26.4** (Fabric и NeoForge) под новый рендер
(OpenGL и Vulkan). Убирает лишнюю работу игры и не меняет игровой процесс.

- **Плавная смена дальности прорисовки** (включено). Ванилла при любой смене дальности выбрасывает все готовые
  чанки, строит видимый мир заново и на несколько кадров вообще не показывает ландшафт. Мод переносит готовую
  геометрию в новую сетку: при увеличении строится только новое кольцо чанков, при уменьшении лишнее убирается,
  мир не пропадает. На 26.4 самый длинный кадр при переключениях — 58 мс вместо 140, пустых кадров — 0 вместо 160.
- **Динамическая дальность** (выключено). Пока FPS ниже цели, рисуемая дальность снижается по одному чанку;
  когда запас появляется — возвращается. Настройка в игре и запрос к серверу не меняются, чанки остаются загруженными.
- **Встроенный бенчмарк**: `/hinexbench run`, `/hinexbench rdswitch`, `/hinexbench stats`
  (на Fabric для команды нужен Fabric API, самому моду — нет).

Настройки — `config/hinexoptimize.properties` (таблица ключей выше). Сборка — `.\gradlew build`.
План и замеры — [docs/PLAN.md](docs/PLAN.md), [docs/BENCHMARKS.md](docs/BENCHMARKS.md).
