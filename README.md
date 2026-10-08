# OsirisMaps

Addon de cliente y plugin de servidor para que el minimapa y el mapa del mundo funcionen como en OsirisMC: interfaz propia, reglas desde el servidor y un mapa por backend de la network.

El cliente no sustituye a Xaero. Se apoya en **Xaero's Minimap** y **Xaero's World Map**.

## Qué incluye

| Pieza | Archivo | Dónde va |
| --- | --- | --- |
| Mod Fabric (cliente) | `osiris-maps-2.0.0.jar` | carpeta `mods` del cliente |
| Plugin Paper | `osiris-maps-plugin-1.0.0.jar` | carpeta `plugins` del servidor |

Los jars de cada versión están en [Releases](https://github.com/FlamingIzanagi/Osiris-Min-WorldMap/releases).

## Cliente (`osiris-maps`)

Minecraft **1.21.11**, Fabric, Java 21.

- Marca OsirisMinimap / OsirisWorldMap y textos de la UI en español.
- Superficie forzada excepto en el Nether, donde el mapa sigue el techo de cueva de forma automática.
- Radar de entidades (mobs) desactivado. La flecha del jugador local se mantiene.
- Los demás jugadores solo se ven si el servidor lo permite (`/omap players`).
- Waypoints y puntos de muerte se pueden bloquear desde el servidor.
- Teclas de Xaero ocultas en Controles; la config sigue en el menú del mod.
- Compatible con FancyMenu.

Dependencias en el cliente: Fabric API, Xaero's Minimap 26.6.0 o superior, Xaero's World Map 1.47.0 o superior.

## Servidor (`osiris-maps-plugin`)

Paper **1.21.8**. Permiso `osirismaps.admin` (OP). El estado de los comandos se guarda en `plugins/OsirisMaps/state.txt` y no se borra con `/omap reload`.

```
/omap display <minimap|worldmap> [jugador] <on|off>
/omap bypass <minimap|worldmap> <on|off>
/omap waypoints <on|off> [mundo]
/omap deathpoints <on|off> [mundo]
/omap players <on|off> [mundo]
/omap playershead <on|off>
/omap playersrange <default|full> [mundo]
/omap waypoint set|remove|list
/omap track start|stop
/omap reload
```

Por defecto: minimapa, world map, waypoints y deathpoints en on; otros jugadores ocultos; iconos en punto (no cabeza); alcance `default` (solo jugadores ya cargados por Minecraft).

## Compilar

```bat
gradlew.bat :osiris-minimap:remapJar
gradlew.bat :osiris-maps-plugin:jar
```
