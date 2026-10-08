package dev.osiris.maps.plugin.util;

import java.util.Locale;
import java.util.Map;

/** Mismos indices que el enum de color de Xaero. */
public final class Colors {
    private static final Map<String, Integer> NAMES = Map.ofEntries(
            Map.entry("black", 0), Map.entry("negro", 0),
            Map.entry("dark_blue", 1), Map.entry("azul_oscuro", 1),
            Map.entry("dark_green", 2), Map.entry("verde_oscuro", 2),
            Map.entry("dark_aqua", 3), Map.entry("cian_oscuro", 3),
            Map.entry("dark_red", 4), Map.entry("rojo_oscuro", 4),
            Map.entry("dark_purple", 5), Map.entry("morado_oscuro", 5),
            Map.entry("gold", 6), Map.entry("dorado", 6), Map.entry("naranja", 6),
            Map.entry("gray", 7), Map.entry("gris", 7),
            Map.entry("dark_gray", 8), Map.entry("gris_oscuro", 8),
            Map.entry("blue", 9), Map.entry("azul", 9),
            Map.entry("green", 10), Map.entry("verde", 10),
            Map.entry("aqua", 11), Map.entry("cian", 11),
            Map.entry("red", 12), Map.entry("rojo", 12),
            Map.entry("purple", 13), Map.entry("morado", 13),
            Map.entry("yellow", 14), Map.entry("amarillo", 14),
            Map.entry("white", 15), Map.entry("blanco", 15),
            Map.entry("magenta", 16),
            Map.entry("light_blue", 17), Map.entry("celeste", 17),
            Map.entry("lime", 18), Map.entry("lima", 18),
            Map.entry("pink", 19), Map.entry("rosa", 19),
            Map.entry("brown", 20), Map.entry("marron", 20), Map.entry("cafe", 20)
    );

    private Colors() {
    }

    public static Integer parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            int index = Integer.parseInt(token.trim());
            if (index >= 0 && index <= 20) {
                return index;
            }
        } catch (NumberFormatException ignored) {
            // El token es un nombre, no un indice.
        }
        return NAMES.get(token.trim().toLowerCase(Locale.ROOT));
    }

    public static boolean known(String token) {
        return parse(token) != null;
    }

    public static Iterable<String> names() {
        return NAMES.keySet();
    }
}
