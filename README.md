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

## Конфиг

`config/hinexoptimize.properties` создаётся при первом запуске. Каждая оптимизация —
свой ключ `mixin.<раздел>`; выключение раздела выключает все его миксины.
Опции с пометкой `[risky]` могут отличаться от ванильного поведения и выключены по умолчанию.

## Лицензия

LGPL-3.0-only, см. [LICENSE](LICENSE).
