package dev.osiris.minimap.net;

import dev.osiris.minimap.OsirisMinimapClient;
import dev.osiris.minimap.server.ServerIdentity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Una copia de las reglas por backend. La huella es la del servidor, no la IP del proxy.
 * Si ese servidor nunca envió reglas, se quedan las de fábrica.
 */
public final class RuleProfile {
    private static String greeted;

    private RuleProfile() {
    }

    public static void enter() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || ServerIdentity.localWorld()) {
            return;
        }
        RemoteWaypoints.clear();
        MapVisibility.reset();
        MapRules.reset();
        String fingerprint = ServerIdentity.current();
        int revision = load(fingerprint);
        if (fingerprint != null && fingerprint.equals(greeted)) {
            return;
        }
        if (!ClientPlayNetworking.canSend(OsirisPayloads.HelloPayload.TYPE)) {
            return;
        }
        ClientPlayNetworking.send(new OsirisPayloads.HelloPayload(revision));
        greeted = fingerprint;
    }

    public static void forget() {
        greeted = null;
    }

    public static void remember(int revision) {
        if (revision <= 0 || ServerIdentity.localWorld()) {
            return;
        }
        String fingerprint = ServerIdentity.current();
        if (fingerprint == null || fingerprint.isBlank()) {
            return;
        }
        MapRules.View rules = MapRules.view();
        StringBuilder text = new StringBuilder();
        text.append("V\t").append(revision).append('\n');
        text.append("M\t").append(MapVisibility.minimap() ? '1' : '0').append('\n');
        text.append("W\t").append(MapVisibility.worldMap() ? '1' : '0').append('\n');
        text.append("H\t").append(PlayerMarks.heads() ? '1' : '0').append('\n');
        text.append("C\twaypoints\t").append(rules.waypointsGlobal() ? '1' : '0').append('\n');
        text.append("C\tdeathpoints\t").append(rules.deathGlobal() ? '1' : '0').append('\n');
        text.append("C\tplayers\t").append(rules.playersGlobal() ? '1' : '0').append('\n');
        text.append("C\trange\t").append(rules.rangeFull() ? '1' : '0').append('\n');
        writeWorlds(text, "waypoints", rules.waypointWorlds());
        writeWorlds(text, "deathpoints", rules.deathWorlds());
        writeWorlds(text, "players", rules.playerWorlds());
        writeWorlds(text, "range", rules.rangeWorlds());
        for (RemoteWaypoints.StaticPoint point : RemoteWaypoints.staticPoints()) {
            text.append("P\t")
                    .append(clean(point.id())).append('\t')
                    .append(clean(point.world())).append('\t')
                    .append(point.x()).append('\t')
                    .append(point.y()).append('\t')
                    .append(point.z()).append('\t')
                    .append(point.color()).append('\t')
                    .append(clean(point.name()))
                    .append('\n');
        }
        Path file = file(fingerprint);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text.toString());
        } catch (IOException error) {
            OsirisMinimapClient.LOGGER.warn("No se pudo guardar las reglas de {}", fingerprint, error);
        }
    }

    private static int load(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return 0;
        }
        Path file = file(fingerprint);
        if (!Files.isRegularFile(file)) {
            return 0;
        }
        boolean minimap = true;
        boolean worldMap = true;
        boolean heads = false;
        boolean waypoints = true;
        boolean deaths = true;
        boolean players = false;
        boolean range = false;
        Map<String, Boolean> waypointWorlds = new HashMap<>();
        Map<String, Boolean> deathWorlds = new HashMap<>();
        Map<String, Boolean> playerWorlds = new HashMap<>();
        Map<String, Boolean> rangeWorlds = new HashMap<>();
        List<RemoteWaypoints.StaticPoint> points = new ArrayList<>();
        int revision = 0;
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank() || line.charAt(0) == '#') {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                switch (parts[0]) {
                    case "V" -> revision = parts.length > 1 ? parseInt(parts[1]) : 0;
                    case "M" -> minimap = flag(parts);
                    case "W" -> worldMap = flag(parts);
                    case "H" -> heads = flag(parts);
                    case "C" -> {
                        if (parts.length < 3) {
                            break;
                        }
                        boolean enabled = "1".equals(parts[2]);
                        switch (parts[1]) {
                            case "waypoints" -> waypoints = enabled;
                            case "deathpoints" -> deaths = enabled;
                            case "players" -> players = enabled;
                            case "range" -> range = enabled;
                            default -> {
                            }
                        }
                    }
                    case "D" -> {
                        if (parts.length < 4) {
                            break;
                        }
                        boolean enabled = "1".equals(parts[3]);
                        switch (parts[1]) {
                            case "waypoints" -> waypointWorlds.put(parts[2], enabled);
                            case "deathpoints" -> deathWorlds.put(parts[2], enabled);
                            case "players" -> playerWorlds.put(parts[2], enabled);
                            case "range" -> rangeWorlds.put(parts[2], enabled);
                            default -> {
                            }
                        }
                    }
                    case "P" -> {
                        if (parts.length < 8) {
                            break;
                        }
                        points.add(new RemoteWaypoints.StaticPoint(
                                parts[1],
                                parts[2],
                                parseDouble(parts[3]),
                                parseDouble(parts[4]),
                                parseDouble(parts[5]),
                                parseInt(parts[6]),
                                parts[7]));
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException | RuntimeException error) {
            return 0;
        }
        MapVisibility.minimap(minimap);
        MapVisibility.worldMap(worldMap);
        PlayerMarks.restore(heads);
        MapRules.replace(waypoints, waypointWorlds, deaths, deathWorlds, players, playerWorlds, range, rangeWorlds);
        for (RemoteWaypoints.StaticPoint point : points) {
            RemoteWaypoints.add(point.id(), point.world(), point.x(), point.y(), point.z(), point.color(), point.name(), false);
        }
        return revision;
    }

    private static void writeWorlds(StringBuilder text, String rule, Map<String, Boolean> worlds) {
        for (Map.Entry<String, Boolean> entry : worlds.entrySet()) {
            text.append("D\t")
                    .append(rule).append('\t')
                    .append(clean(entry.getKey())).append('\t')
                    .append(Boolean.TRUE.equals(entry.getValue()) ? '1' : '0')
                    .append('\n');
        }
    }

    private static boolean flag(String[] parts) {
        return parts.length > 1 && "1".equals(parts[1]);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException error) {
            return 0.0;
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static Path file(String fingerprint) {
        StringBuilder safe = new StringBuilder(fingerprint.length());
        for (int i = 0; i < fingerprint.length(); i++) {
            char c = fingerprint.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
                safe.append(c);
            } else {
                safe.append('_');
            }
        }
        return FabricLoader.getInstance().getConfigDir().resolve("osiris_minimap").resolve("servers").resolve(safe + ".txt");
    }
}
