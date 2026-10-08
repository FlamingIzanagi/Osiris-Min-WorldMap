package dev.osiris.minimap.maps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;

/**
 * Lee y borra los mapas de {@code xaero/world-map}. Cada huella de backend es un mapa.
 * El minimapa del mismo backend se borra junto con él.
 */
public final class SavedMapStore {
    private SavedMapStore() {
    }

    public static List<SavedMap> load() {
        Path root = worldMapRoot();
        if (root == null || !Files.isDirectory(root)) {
            return List.of();
        }
        List<Draft> drafts = new ArrayList<>();
        try (Stream<Path> servers = Files.list(root)) {
            for (Path server : servers.filter(Files::isDirectory).sorted().toList()) {
                if (Files.isRegularFile(server.resolve("server_config.txt"))) {
                    drafts.add(draft(server, server));
                    continue;
                }
                try (Stream<Path> children = Files.list(server)) {
                    for (Path child : children.filter(Files::isDirectory).sorted().toList()) {
                        if (Files.isRegularFile(child.resolve("server_config.txt"))) {
                            drafts.add(draft(server, child));
                        }
                    }
                }
            }
        } catch (IOException error) {
            return List.of();
        }
        return name(drafts);
    }

    public static boolean isLoaded(SavedMap map) {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null || !session.isUsable()) {
            return false;
        }
        MapProcessor processor = session.getMapProcessor();
        return processor != null && map.loadedIn(processor.getCurrentWorldId());
    }

    /** @return mensaje de error, o null si el mapa se borró */
    public static String delete(SavedMap map) {
        Path root = worldMapRoot();
        if (root == null) {
            return "No se encontró la carpeta de mapas.";
        }
        Path world = map.worldPath().toAbsolutePath().normalize();
        if (!world.startsWith(root.toAbsolutePath().normalize())) {
            return "La ruta del mapa no es válida.";
        }
        if (isLoaded(map)) {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            if (session != null && session.getMapProcessor() != null) {
                session.getMapProcessor().requestCurrentMapDeletion();
            }
        }
        try {
            deleteTree(world);
            Path minimap = map.minimapPath();
            if (minimap != null) {
                Path miniRoot = root.getParent() == null ? null : root.getParent().resolve("minimap").toAbsolutePath().normalize();
                Path mini = minimap.toAbsolutePath().normalize();
                if (miniRoot != null && mini.startsWith(miniRoot)) {
                    deleteTree(mini);
                }
            }
        } catch (IOException error) {
            return "No se pudo borrar el mapa. Si estás dentro de ese mundo, sal y vuelve a intentarlo.";
        }
        if (Files.exists(world)) {
            return "No se pudo borrar el mapa. Si estás dentro de ese mundo, sal y vuelve a intentarlo.";
        }
        return null;
    }

    private static List<SavedMap> name(List<Draft> drafts) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Draft draft : drafts) {
            counts.merge(draft.server + "\0" + draft.title, 1, Integer::sum);
        }
        List<SavedMap> maps = new ArrayList<>();
        for (Draft draft : drafts) {
            String title = draft.title;
            if (counts.getOrDefault(draft.server + "\0" + title, 0) > 1 && !draft.shortId.isEmpty()) {
                title = title + " (" + draft.shortId + ")";
            }
            maps.add(new SavedMap(draft.server, title, draft.world, draft.minimap, draft.worldId));
        }
        return maps;
    }

    private static Draft draft(Path serverDir, Path mapDir) {
        String folder = serverDir.getFileName().toString();
        String server = folder.startsWith("Multiplayer_") ? folder.substring("Multiplayer_".length()) : folder;
        String token = mapDir.equals(serverDir) ? "" : mapDir.getFileName().toString();
        String worldId = token.isEmpty() ? folder : folder + "/" + token;
        Path minimap = null;
        if (!token.isEmpty() && serverDir.getParent() != null) {
            Path candidate = serverDir.getParent().resolve("minimap").resolve(folder + "__" + token);
            if (Files.isDirectory(candidate)) {
                minimap = candidate;
            }
        }
        return new Draft(server, displayTitle(mapDir, token), shortId(token), mapDir, minimap, worldId);
    }

    private static String displayTitle(Path mapDir, String token) {
        String base = title(mapDir);
        String radius = radius(token);
        if ("Mapa".equals(base) && !radius.isEmpty()) {
            return "Mapa (" + radius + " chunks)";
        }
        return base;
    }

    private static String radius(String token) {
        int mark = token.indexOf("-r");
        if (mark < 0 || mark + 2 >= token.length()) {
            return "";
        }
        int start = mark + 2;
        int end = start;
        while (end < token.length() && Character.isDigit(token.charAt(end))) {
            end++;
        }
        return end == start ? "" : token.substring(start, end);
    }

    private static String title(Path mapDir) {
        List<String> dimensions = new ArrayList<>();
        try (Stream<Path> children = Files.list(mapDir)) {
            for (Path child : children.filter(Files::isDirectory).sorted().toList()) {
                String name = child.getFileName().toString();
                if (name.startsWith("mw$") || name.equals("cache") || name.equals("caves")) {
                    continue;
                }
                if (Files.isDirectory(child.resolve("mw$default")) || Files.isRegularFile(child.resolve("dimension_config.txt"))) {
                    dimensions.add(prettyDimension(name));
                }
            }
        } catch (IOException ignored) {
            return "Mapa";
        }
        if (dimensions.isEmpty()) {
            return "Mapa";
        }
        return String.join(", ", dimensions);
    }

    private static String prettyDimension(String folder) {
        return switch (folder) {
            case "null", "dim%0", "minecraft$overworld" -> "Mapa";
            case "dim%-1", "minecraft$the_nether" -> "Nether";
            case "dim%1", "minecraft$the_end" -> "End";
            default -> folder;
        };
    }

    private static String shortId(String token) {
        int start = token.startsWith("s") ? 1 : 0;
        int end = token.indexOf("-r");
        if (end <= start) {
            return "";
        }
        String seed = token.substring(start, end);
        return seed.length() <= 6 ? seed : seed.substring(0, 6);
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            IOException failure = null;
            for (Path path : paths) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    failure = error;
                }
            }
            if (failure != null && Files.exists(root)) {
                throw failure;
            }
        }
    }

    private static Path worldMapRoot() {
        if (WorldMap.saveFolder != null) {
            return WorldMap.saveFolder.toPath();
        }
        return Minecraft.getInstance().gameDirectory.toPath().resolve("xaero").resolve("world-map");
    }

    private record Draft(String server, String title, String shortId, Path world, Path minimap, String worldId) {
    }
}
