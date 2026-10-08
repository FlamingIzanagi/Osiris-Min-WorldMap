package dev.osiris.minimap.maps;

import net.minecraft.client.gui.screens.Screen;

/** Marca que el world map abierto es una previsualización, no el mapa normal. */
public final class MapPreview {
    private static SavedMap selected;
    private static Screen returnTo;
    private static Screen listParent;
    private static boolean keepOpen;

    private MapPreview() {
    }

    public static void begin(SavedMap map, Screen management, Screen waypoints) {
        selected = map;
        returnTo = management;
        listParent = waypoints;
        keepOpen = false;
    }

    public static boolean active() {
        return selected != null;
    }

    public static SavedMap selected() {
        return selected;
    }

    public static Screen returnTo() {
        return returnTo;
    }

    public static Screen listParent() {
        return listParent;
    }

    /** El cierre del mapa va al diálogo de borrado: no soltar el mapa todavía. */
    public static void keepForDelete() {
        keepOpen = true;
    }

    public static void end() {
        if (keepOpen) {
            keepOpen = false;
            return;
        }
        selected = null;
        returnTo = null;
        listParent = null;
    }

    public static void clear() {
        keepOpen = false;
        selected = null;
        returnTo = null;
        listParent = null;
    }
}
