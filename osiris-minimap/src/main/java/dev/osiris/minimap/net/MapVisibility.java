package dev.osiris.minimap.net;

/** Lo que el servidor decidió para este cliente. Sin paquete, los dos mapas siguen visibles. */
public final class MapVisibility {
    private static boolean minimap = true;
    private static boolean worldMap = true;

    private MapVisibility() {
    }

    public static boolean minimap() {
        return minimap;
    }

    public static boolean worldMap() {
        return worldMap;
    }

    public static void minimap(boolean visible) {
        minimap = visible;
    }

    public static void worldMap(boolean visible) {
        worldMap = visible;
    }

    public static void reset() {
        minimap = true;
        worldMap = true;
    }
}
