package dev.osiris.minimap.brand;

/**
 * Sustituye los nombres visibles de Xaero's. No toca claves de traducción ni ids de recurso:
 * cambiar {@code gui.xaero_*} rompería los keybinds y los archivos de idioma del mod original.
 */
public final class OsirisText {
    private OsirisText() {
    }

    public static String rebrand(String value) {
        if (value == null || value.indexOf("Xaero") < 0) {
            return value;
        }
        return value
                .replace("Xaero's Minimap", "OsirisMinimap")
                .replace("Xaero's World Map", "OsirisWorldMap")
                .replace("Xaero's WorldMap", "OsirisWorldMap")
                .replace("Xaero's minimap", "OsirisMinimap")
                .replace("Xaero's world map", "OsirisWorldMap")
                .replace("Xaero\u2019s Minimap", "OsirisMinimap")
                .replace("Xaero\u2019s World Map", "OsirisWorldMap");
    }
}
