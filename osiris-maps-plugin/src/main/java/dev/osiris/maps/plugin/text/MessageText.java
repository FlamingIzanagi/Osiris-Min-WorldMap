package dev.osiris.maps.plugin.text;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** Acepta & clasico, &#RRGGBB, &x&R&R&G&G&B&B y MiniMessage en el mismo texto. */
public final class MessageText {
    private static final MiniMessage MINI = MiniMessage.builder().strict(false).build();
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY_HEX = Pattern.compile("(?i)&x(?:&[0-9a-fA-F]){6}");
    private static final Map<Character, String> LEGACY = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>")
    );

    private MessageText() {
    }

    public static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        String text = raw.replace('§', '&');
        text = HEX.matcher(text).replaceAll("<#$1>");
        Matcher legacyHex = LEGACY_HEX.matcher(text);
        StringBuilder converted = new StringBuilder();
        while (legacyHex.find()) {
            String digits = legacyHex.group().substring(2).replace("&", "");
            legacyHex.appendReplacement(converted, "<#" + digits + ">");
        }
        legacyHex.appendTail(converted);
        text = legacy(converted.toString());
        return MINI.deserialize(text);
    }

    public static String fill(String template, String... tokens) {
        String text = template == null ? "" : template;
        for (int i = 0; i + 1 < tokens.length; i += 2) {
            String value = tokens[i + 1] == null ? "" : tokens[i + 1];
            text = text.replace("{" + tokens[i] + "}", escape(value));
        }
        return text;
    }

    private static String legacy(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '&' && i + 1 < text.length()) {
                String tag = LEGACY.get(Character.toLowerCase(text.charAt(i + 1)));
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(current);
        }
        return out.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("<", "\\<").replace(">", "\\>");
    }
}
