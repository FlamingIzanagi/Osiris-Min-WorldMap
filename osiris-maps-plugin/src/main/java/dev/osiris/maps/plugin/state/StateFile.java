package dev.osiris.maps.plugin.state;

import dev.osiris.maps.plugin.OsirisMapsPlugin;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Texto plano. La escritura ocurre fuera del hilo del servidor, sobre una copia ya armada. */
public final class StateFile {
    private StateFile() {
    }

    public record Snapshot(
            Map<UUID, EnumMap<MapState.Kind, Boolean>> display,
            Map<UUID, EnumMap<MapState.Kind, Boolean>> bypass,
            Map<String, MapState.Waypoint> waypoints,
            Map<UUID, MapState.Track> tracks,
            Map<MapState.Rule, Boolean> globalRules,
            Map<MapState.Rule, Map<String, Boolean>> worldRules,
            Map<MapState.Kind, Boolean> globalDisplay,
            Boolean playerHeads,
            Boolean rangeFull,
            Map<String, Boolean> rangeWorlds,
            long revision
    ) {
    }

    public static Snapshot read(OsirisMapsPlugin plugin) {
        Path file = file(plugin);
        Map<UUID, EnumMap<MapState.Kind, Boolean>> display = new LinkedHashMap<>();
        Map<UUID, EnumMap<MapState.Kind, Boolean>> bypass = new LinkedHashMap<>();
        Map<String, MapState.Waypoint> waypoints = new LinkedHashMap<>();
        Map<UUID, MapState.Track> tracks = new LinkedHashMap<>();
        Map<MapState.Rule, Boolean> globalRules = new EnumMap<>(MapState.Rule.class);
        Map<MapState.Rule, Map<String, Boolean>> worldRules = new EnumMap<>(MapState.Rule.class);
        Map<MapState.Kind, Boolean> globalDisplay = new EnumMap<>(MapState.Kind.class);
        Boolean playerHeads = null;
        Boolean rangeFull = null;
        Map<String, Boolean> rangeWorlds = new LinkedHashMap<>();
        long revision = 1L;
        if (!Files.isRegularFile(file)) {
            return new Snapshot(display, bypass, waypoints, tracks, globalRules, worldRules, globalDisplay, playerHeads, rangeFull, rangeWorlds, revision);
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                char kind = line.charAt(0);
                String body = line.length() > 2 ? line.substring(2) : "";
                switch (kind) {
                    case 'D' -> readFlag(display, body);
                    case 'B' -> readFlag(bypass, body);
                    case 'W' -> {
                        MapState.Waypoint waypoint = readWaypoint(body);
                        if (waypoint != null) {
                            waypoints.put(waypoint.id(), waypoint);
                        }
                    }
                    case 'T' -> {
                        MapState.Track track = MapState.Track.parse(body);
                        if (track != null) {
                            tracks.put(track.playerId(), track);
                        }
                    }
                    case 'C' -> readRule(globalRules, worldRules, body);
                    case 'G' -> readGlobalDisplay(globalDisplay, body);
                    case 'H' -> playerHeads = Boolean.parseBoolean(body.trim());
                    case 'R' -> {
                        String[] parts = body.split("\t");
                        if (parts.length >= 2) {
                            boolean full = parts[1].equalsIgnoreCase("full");
                            if ("*".equals(parts[0])) {
                                rangeFull = full;
                            } else {
                                rangeWorlds.put(parts[0], full);
                            }
                        }
                    }
                    case 'V' -> {
                        try {
                            revision = Long.parseLong(body.trim());
                        } catch (NumberFormatException ignored) {
                            revision = 1L;
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException | RuntimeException error) {
            plugin.getLogger().warning("No se pudo leer el estado de OsirisMaps: " + error.getMessage());
        }
        return new Snapshot(display, bypass, waypoints, tracks, globalRules, worldRules, globalDisplay, playerHeads, rangeFull, rangeWorlds, revision);
    }

    public static void write(OsirisMapsPlugin plugin, Snapshot snapshot) {
        Path file = file(plugin);
        StringBuilder text = new StringBuilder();
        text.append("# OsirisMaps 2.0.0\n");
        text.append('V').append('\t').append(snapshot.revision()).append('\n');
        writeFlags(text, 'D', snapshot.display());
        writeFlags(text, 'B', snapshot.bypass());
        for (Map.Entry<MapState.Kind, Boolean> entry : snapshot.globalDisplay().entrySet()) {
            text.append('G').append('\t').append(entry.getKey().name()).append('\t').append(entry.getValue()).append('\n');
        }
        if (snapshot.playerHeads() != null) {
            text.append('H').append('\t').append(snapshot.playerHeads()).append('\n');
        }
        if (snapshot.rangeFull() != null) {
            text.append('R').append('\t').append("*\t").append(snapshot.rangeFull() ? "full" : "default").append('\n');
        }
        if (snapshot.rangeWorlds() != null) {
            for (Map.Entry<String, Boolean> world : snapshot.rangeWorlds().entrySet()) {
                text.append('R').append('\t')
                        .append(world.getKey()).append('\t')
                        .append(Boolean.TRUE.equals(world.getValue()) ? "full" : "default")
                        .append('\n');
            }
        }
        for (MapState.Waypoint waypoint : snapshot.waypoints().values()) {
            text.append('W').append('\t')
                    .append(waypoint.id()).append('\t')
                    .append(waypoint.world()).append('\t')
                    .append(waypoint.x()).append('\t')
                    .append(waypoint.y()).append('\t')
                    .append(waypoint.z()).append('\t')
                    .append(waypoint.color()).append('\t')
                    .append(waypoint.name().replace('\n', ' ').replace('\t', ' '))
                    .append('\n');
        }
        for (MapState.Track track : snapshot.tracks().values()) {
            text.append('T').append('\t').append(track.fileLine()).append('\n');
        }
        for (Map.Entry<MapState.Rule, Boolean> entry : snapshot.globalRules().entrySet()) {
            text.append('C').append('\t').append(entry.getKey().name()).append("\t*\t").append(entry.getValue()).append('\n');
        }
        for (Map.Entry<MapState.Rule, Map<String, Boolean>> entry : snapshot.worldRules().entrySet()) {
            for (Map.Entry<String, Boolean> world : entry.getValue().entrySet()) {
                text.append('C').append('\t')
                        .append(entry.getKey().name()).append('\t')
                        .append(world.getKey()).append('\t')
                        .append(world.getValue()).append('\n');
            }
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, text.toString());
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomic) {
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException error) {
                plugin.getLogger().warning("No se pudo guardar el estado de OsirisMaps: " + error.getMessage());
            }
        }
    }

    private static void readFlag(Map<UUID, EnumMap<MapState.Kind, Boolean>> target, String body) {
        String[] parts = body.split("\t");
        if (parts.length < 3) {
            return;
        }
        MapState.Kind kind = MapState.Kind.valueOf(parts[1].toUpperCase(Locale.ROOT));
        target.computeIfAbsent(UUID.fromString(parts[0]), ignored -> new EnumMap<>(MapState.Kind.class))
                .put(kind, Boolean.parseBoolean(parts[2]));
    }

    private static void readGlobalDisplay(Map<MapState.Kind, Boolean> target, String body) {
        String[] parts = body.split("\t");
        if (parts.length < 2) {
            return;
        }
        target.put(MapState.Kind.valueOf(parts[0].toUpperCase(Locale.ROOT)), Boolean.parseBoolean(parts[1]));
    }

    private static void readRule(
            Map<MapState.Rule, Boolean> globalRules,
            Map<MapState.Rule, Map<String, Boolean>> worldRules,
            String body
    ) {
        String[] parts = body.split("\t");
        if (parts.length < 3) {
            return;
        }
        MapState.Rule rule = MapState.Rule.valueOf(parts[0].toUpperCase(Locale.ROOT));
        boolean enabled = Boolean.parseBoolean(parts[2]);
        if ("*".equals(parts[1])) {
            globalRules.put(rule, enabled);
            return;
        }
        worldRules.computeIfAbsent(rule, ignored -> new LinkedHashMap<>()).put(parts[1], enabled);
    }

    private static MapState.Waypoint readWaypoint(String body) {
        String[] parts = body.split("\t", 7);
        if (parts.length < 7) {
            return null;
        }
        return new MapState.Waypoint(
                parts[0],
                parts[1],
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Double.parseDouble(parts[4]),
                Integer.parseInt(parts[5]),
                parts[6]
        );
    }

    private static void writeFlags(StringBuilder text, char code, Map<UUID, EnumMap<MapState.Kind, Boolean>> flags) {
        for (Map.Entry<UUID, EnumMap<MapState.Kind, Boolean>> entry : flags.entrySet()) {
            for (Map.Entry<MapState.Kind, Boolean> flag : entry.getValue().entrySet()) {
                text.append(code).append('\t')
                        .append(entry.getKey()).append('\t')
                        .append(flag.getKey().name()).append('\t')
                        .append(flag.getValue()).append('\n');
            }
        }
    }

    private static Path file(OsirisMapsPlugin plugin) {
        return plugin.getDataFolder().toPath().resolve("state.txt");
    }
}
