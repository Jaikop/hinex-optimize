# Changelog

## 0.1.0 — 2026-10-16

First release. Minecraft 26.2 and 26.3 (Fabric, NeoForge), 26.4-snapshot-4 (Fabric).

- **Smooth render distance** (on): changing the render distance moves the built chunk sections into the new
  grid instead of rebuilding everything. Raising it builds only the new ring of chunks, lowering it drops the
  far ones, and the world no longer disappears for a few frames.
- **Dynamic render distance** (off, `mixin.render.dynamicview`): lowers the rendered distance one chunk at a
  time while FPS stays below `dynamicview.target_fps` and raises it back when there is headroom.
- **`/hinexbench`**: in-game benchmark (`run`, `rdswitch`, `reload`, `stats`).
- Config file `config/hinexoptimize.properties` with a switch for every feature.
