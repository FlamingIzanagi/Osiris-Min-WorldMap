# OsirisMaps

Client addon and server plugin so the minimap and world map behave like they do on OsirisMC: branded UI, server-side rules, and one map per network backend.

This client does not replace Xaero. It runs on top of **Xaero's Minimap** and **Xaero's World Map**.

## What's included

| Piece | File | Installs in |
| --- | --- | --- |
| Fabric client mod | `osiris-maps-2.0.0.jar` | client `mods` folder |
| Paper plugin | `osiris-maps-plugin-1.0.0.jar` | server `plugins` folder |

Jars for each version are in [Releases](https://github.com/FlamingIzanagi/Osiris-Min-WorldMap/releases).

## Client (`osiris-maps`)

Minecraft **1.21.11**, Fabric, Java 21.

- OsirisMinimap / OsirisWorldMap branding and Spanish UI copy.
- Forced surface everywhere except the Nether, where cave mode follows the ceiling automatically.
- Entity radar (mobs) is off. The local player's arrow stays.
- Other players only show if the server allows it (`/omap players`).
- Waypoints and deathpoints can be locked by the server.
- Xaero keybinds are hidden in Controls; settings stay in the mod menu.
- Compatible with FancyMenu.

Client dependencies: Fabric API, Xaero's Minimap 26.6.0 or newer, Xaero's World Map 1.47.0 or newer.

## Server (`osiris-maps-plugin`)

Paper **1.21.8**. Permission `osirismaps.admin` (OP). Command state is stored in `plugins/OsirisMaps/state.txt` and is not wiped by `/omap reload`.

```
/omap display <minimap|worldmap> [player] <on|off>
/omap bypass <minimap|worldmap> <on|off>
/omap waypoints <on|off> [world]
/omap deathpoints <on|off> [world]
/omap players <on|off> [world]
/omap playershead <on|off>
/omap playersrange <default|full> [world]
/omap waypoint set|remove|list
/omap track start|stop
/omap reload
```

Defaults: minimap, world map, waypoints, and deathpoints on; other players hidden; dots instead of heads; `default` range (only players Minecraft has already loaded).

## Build

```bat
gradlew.bat :osiris-minimap:remapJar
gradlew.bat :osiris-maps-plugin:jar
```
