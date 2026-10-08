package dev.osiris.minimap.config;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Mueve {@code config/xaero/minimap} y {@code config/xaero/world-map} a
 * {@code config/osiris_minimap} y {@code config/osiris_worldmap}.
 * Las rutas que no pasan por la carpeta {@code config} (caché del mapa) no se tocan.
 */
public final class OsirisPaths {
    private OsirisPaths() {
    }

    public static Path relocate(Path original, String folderName) {
        if (original == null || folderName == null || folderName.isEmpty()) {
            return original;
        }
        ArrayDeque<String> tail = new ArrayDeque<>();
        Path cursor = original;
        while (cursor != null) {
            Path fileName = cursor.getFileName();
            if (fileName != null && "config".equals(fileName.toString())) {
                Path result = cursor.resolve(folderName);
                boolean droppedXaero = false;
                Iterator<String> parts = tail.descendingIterator();
                while (parts.hasNext()) {
                    String part = parts.next();
                    if (isXaeroSegment(part)) {
                        droppedXaero = true;
                        continue;
                    }
                    result = result.resolve(part);
                }
                return droppedXaero ? result : original;
            }
            if (fileName != null) {
                tail.addLast(fileName.toString());
            }
            cursor = cursor.getParent();
        }
        return original;
    }

    private static boolean isXaeroSegment(String part) {
        return "xaero".equals(part) || "minimap".equals(part) || "world-map".equals(part);
    }
}
