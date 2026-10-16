# Тексты для Modrinth и CurseForge

Короткое описание — поле «Summary» (Modrinth: до 256 символов, CurseForge: до 255).
Полное описание — поле «Description»; обе площадки принимают Markdown.
Иконка проекта — [`docs/assets/icon-512.png`](assets/icon-512.png) (512×512).

Теги/категории: Optimization, Utility; окружение: Client (на сервере не нужен, но и не мешает).
Лицензия: LGPL-3.0-only. Исходники: https://github.com/Jaikop/hinex-optimize

---

## English

### Short description

Change the render distance without rebuilding every chunk: the world never disappears. Optional dynamic render distance keeps your FPS. Minecraft 26.2–26.4, Fabric and NeoForge.

### Description

**HinexOptimize** is a client-side optimization mod for the new Minecraft renderer (OpenGL and Vulkan).
It removes work the game does for nothing and never changes how the game plays.

## Smooth render distance

Every time you change the render distance, vanilla throws away all built chunks, rebuilds the whole
visible world and shows no terrain at all for a few frames. HinexOptimize moves the chunks that are
already built into the new grid instead:

- raising the distance builds **only the new ring** of chunks,
- lowering it simply drops the far ones,
- the world **stays on screen** the whole time.

Measured on Minecraft 26.4 (Vulkan, Intel Iris Xe), switching 12 → 16 → 8 → 12 chunks:

| | Vanilla | HinexOptimize |
|---|---|---|
| Longest frame | 140 ms | **58 ms** |
| Frames over 50 ms | 10 | **1** |
| Chunk sections rebuilt | 1464 | **639** |
| Frames with no terrain | 160 | **0** |

On by default.

## Dynamic render distance

Optional (off by default). While your FPS stays below a target, the rendered distance goes down one
chunk at a time; when there is headroom again, it comes back. Your setting in the options and the
distance requested from the server stay as you set them, so chunks stay loaded and coming back is cheap.

In a test at render distance 24 the mod settled at 16 chunks and raised average FPS from 131 to 145
(1% low from 75 to 85).

Turn it on in `config/hinexoptimize.properties`:

```
mixin.render.dynamicview=true
dynamicview.target_fps=60
dynamicview.min_distance=6
dynamicview.max_reduction=8
```

## Built-in benchmark

- `/hinexbench run [seconds]`: spins the camera and reports average FPS, 1% / 0.1% lows, draw calls and resource reload time
- `/hinexbench rdswitch`: measures render distance switches
- `/hinexbench stats`: live per-frame counters

On Fabric the command needs Fabric API. The mod itself does not.

## Versions

- Minecraft 26.2 and 26.3: Fabric and NeoForge
- Minecraft 26.4-snapshot-4: Fabric
- Java 25

## Compatibility

Not yet tested together with mods that replace the chunk renderer (Sodium, Iris, VulkanMod). Every
feature can be switched off in `config/hinexoptimize.properties`. Found a problem? Please
[open an issue](https://github.com/Jaikop/hinex-optimize/issues) and attach your `latest.log`.

Open source under LGPL-3.0: https://github.com/Jaikop/hinex-optimize

---

## Русский

### Короткое описание

Смена дальности прорисовки без перестройки всех чанков: мир больше не пропадает. Опционально — динамическая дальность для стабильного FPS. Minecraft 26.2–26.4, Fabric и NeoForge.

### Описание

**HinexOptimize** — клиентский мод-оптимизация под новый рендер Minecraft (OpenGL и Vulkan).
Убирает лишнюю работу игры и никак не меняет игровой процесс.

## Плавная смена дальности прорисовки

При каждой смене дальности ванилла выбрасывает все готовые чанки, строит видимый мир заново и на
несколько кадров вообще не показывает ландшафт. HinexOptimize переносит уже готовые чанки в новую сетку:

- при увеличении дальности строится **только новое кольцо** чанков,
- при уменьшении дальние чанки просто убираются,
- мир **не пропадает** с экрана.

Замер на Minecraft 26.4 (Vulkan, Intel Iris Xe), переключения 12 → 16 → 8 → 12 чанков:

| | Ванилла | HinexOptimize |
|---|---|---|
| Самый длинный кадр | 140 мс | **58 мс** |
| Кадров дольше 50 мс | 10 | **1** |
| Перестроено секций чанков | 1464 | **639** |
| Кадров без ландшафта | 160 | **0** |

Включено по умолчанию.

## Динамическая дальность прорисовки

Опционально (по умолчанию выключено). Пока FPS ниже цели, рисуемая дальность снижается по одному чанку;
когда появляется запас — возвращается обратно. Настройка в игре и запрос к серверу не меняются, поэтому
чанки остаются загруженными, и вернуть дальность дёшево.

В тесте на дальности 24 мод остановился на 16 чанках, средний FPS вырос со 131 до 145 (1% low — с 75 до 85).

Включается в `config/hinexoptimize.properties`:

```
mixin.render.dynamicview=true
dynamicview.target_fps=60
dynamicview.min_distance=6
dynamicview.max_reduction=8
```

## Встроенный бенчмарк

- `/hinexbench run [секунды]` — вращает камеру и показывает средний FPS, 1% / 0.1% low, число вызовов отрисовки и время перезагрузки ресурсов
- `/hinexbench rdswitch` — замер смены дальности прорисовки
- `/hinexbench stats` — счётчики за последнюю секунду

На Fabric для команды нужен Fabric API, самому моду — нет.

## Версии

- Minecraft 26.2 и 26.3 — Fabric и NeoForge
- Minecraft 26.4-snapshot-4 — Fabric
- Java 25

## Совместимость

Совместная работа с модами, которые заменяют рендер чанков (Sodium, Iris, VulkanMod), пока не проверена.
Любую функцию можно выключить в `config/hinexoptimize.properties`. Нашли проблему —
[создайте issue](https://github.com/Jaikop/hinex-optimize/issues) и приложите `latest.log`.

Открытый исходный код, LGPL-3.0: https://github.com/Jaikop/hinex-optimize
