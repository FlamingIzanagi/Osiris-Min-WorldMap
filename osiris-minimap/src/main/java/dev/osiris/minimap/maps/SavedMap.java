package dev.osiris.minimap.maps;

import java.nio.file.Path;

/** Un mapa guardado en disco, agrupado por el servidor del que salió. */
public record SavedMap(String server, String title, Path worldPath, Path minimapPath, String worldId) {
    public boolean loadedIn(String currentWorldId) {
        if (currentWorldId == null || worldId == null || worldId.isEmpty()) {
            return false;
        }
        return currentWorldId.equals(worldId) || currentWorldId.startsWith(worldId + "/");
    }
}
