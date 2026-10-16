# HinexOptimize

Мод-оптимизация для Minecraft Java **26.2–26.4** (Fabric и NeoForge).
Ускоряет новый рендер Mojang (`renderpearl`, OpenGL и Vulkan), **не меняя поведения игры**.

План, находки и дорожная карта — в [docs/PLAN.md](docs/PLAN.md).

## Сборка

Нужен JDK 25 (Gradle найдёт Temurin 25 сам или скачает через toolchains).

```
gradlew build
```

Готовые jar-файлы каждой версии лежат в `versions/<версия>-<загрузчик>/build/libs/`.
Собрать всё в одну папку `build/libs/<версия мода>/`:

```
gradlew buildAndCollect
```

## Версии

| Узел | Minecraft | Загрузчик |
|---|---|---|
| `26.2-fabric`, `26.2-neoforge` | 26.2 | Fabric / NeoForge |
| `26.3-fabric`, `26.3-neoforge` | 26.3 | Fabric / NeoForge |
| `26.4-fabric` | 26.4-snapshot-4 | Fabric |

Код общий для всех версий: различия помечаются комментариями Stonecutter
(`//? if fabric {`, `//? if >=26.4 {`). Активная версия в исходниках задаётся
в `stonecutter.gradle.kts` (`stonecutter active "26.3-fabric"`); переключить:

```
gradlew "Set active project to 26.4-fabric"
```

Запуск клиента для проверки: `gradlew :26.3-fabric:runClient` или `gradlew :26.3-neoforge:runClient`.

## Бенчмарк

В игре (нужен Fabric API на Fabric): `/hinexbench run [секунды]` — камера вращается на месте,
мод меряет FPS, 1% / 0.1% low и счётчики отрисовки, затем время перезагрузки ресурсов (F3+T).
`/hinexbench reload` — только перезагрузка, `/hinexbench stats` — счётчики за последнюю секунду.

Автоматический прогон для сравнения сборок (создаёт мир с фиксированным сидом, меряет, закрывает игру):

```
powershell -ExecutionPolicy Bypass -File scripts\bench.ps1 -Node 26.4-fabric -Tag baseline -Vanilla
powershell -ExecutionPolicy Bypass -File scripts\bench.ps1 -Node 26.3-fabric -Backend vulkan -Tag test -Repeat 2
```

`-Vanilla` выключает все оптимизации мода (остаются только замеры). Результаты дописываются
в `run/hinexoptimize/bench/summary-v1.csv`, по кадрам — в отдельные CSV рядом.
Окно игры во время прогона нельзя сворачивать. Сводка замеров — в [docs/BENCHMARKS.md](docs/BENCHMARKS.md).

## Что уже умеет

- **Плавная смена дальности прорисовки** (`mixin.render.smoothview`, включено). Ванилла при любой смене
  дальности выбрасывает все готовые чанки и строит их заново. Мод переносит готовую геометрию в новую сетку:
  при увеличении строится только добавленное кольцо чанков, при уменьшении лишнее просто убирается.
- **Динамическая дальность** (`mixin.render.dynamicview`, выключено по умолчанию). Если FPS падает ниже
  `dynamicview.target_fps`, дальность прорисовки снижается на 1 чанк (не ниже `dynamicview.min_distance` и не
  больше чем на `dynamicview.max_reduction` от вашей настройки); когда запас появляется — возвращается обратно.
  Меняется только то, что рисуется: настройка в игре и то, что запрашивается у сервера, остаются вашими,
  поэтому чанки не выгружаются и вернуть дальность дёшево.

## Конфиг

`config/hinexoptimize.properties` создаётся при первом запуске. Каждая оптимизация —
свой ключ `mixin.<раздел>`; выключение раздела выключает все его миксины.
Опции с пометкой `[risky]` могут отличаться от ванильного поведения и выключены по умолчанию.

## Лицензия

LGPL-3.0-only, см. [LICENSE](LICENSE).
