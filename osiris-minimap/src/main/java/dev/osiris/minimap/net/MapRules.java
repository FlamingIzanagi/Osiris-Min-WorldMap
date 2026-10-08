package dev.osiris.minimap.net;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/** Reglas del servidor. Sin paquete, crear waypoints y puntos de muerte siguen activos, y los jugadores no se muestran. */
public final class MapRules {
    private static boolean waypointsGlobal = true;
    private static boolean deathGlobal = true;
    private static boolean playersGlobal = false;
    private static boolean rangeFullGlobal = false;
    private static final Map<String, Boolean> WAYPOINT_WORLDS = new HashMap<>();
    private static final Map<String, Boolean> DEATH_WORLDS = new HashMap<>();
    private static final Map<String, Boolean> PLAYER_WORLDS = new HashMap<>();
    private static final Map<String, Boolean> RANGE_WORLDS = new HashMap<>();

    private MapRules() {
    }

    public static boolean waypoints() {
        return allowed(waypointsGlobal, WAYPOINT_WORLDS);
    }

    public static boolean deathpoints() {
        return allowed(deathGlobal, DEATH_WORLDS);
    }

    public static boolean players() {
        return allowed(playersGlobal, PLAYER_WORLDS);
    }

    /** true = el servidor manda a todos. false = solo los jugadores que el cliente ya tiene cargados. */
    public static boolean fullTracking() {
        return allowed(rangeFullGlobal, RANGE_WORLDS);
    }

    public static void replace(
            boolean waypoints,
            Map<String, Boolean> waypointWorlds,
            boolean deaths,
            Map<String, Boolean> deathWorlds,
            boolean players,
            Map<String, Boolean> playerWorlds,
            boolean rangeFull,
            Map<String, Boolean> rangeWorlds
    ) {
        waypointsGlobal = waypoints;
        deathGlobal = deaths;
        playersGlobal = players;
        rangeFullGlobal = rangeFull;
        WAYPOINT_WORLDS.clear();
        WAYPOINT_WORLDS.putAll(waypointWorlds);
        DEATH_WORLDS.clear();
        DEATH_WORLDS.putAll(deathWorlds);
        PLAYER_WORLDS.clear();
        PLAYER_WORLDS.putAll(playerWorlds);
        RANGE_WORLDS.clear();
        RANGE_WORLDS.putAll(rangeWorlds);
    }

    public static void reset() {
        waypointsGlobal = true;
        deathGlobal = true;
        playersGlobal = false;
        rangeFullGlobal = false;
        WAYPOINT_WORLDS.clear();
        DEATH_WORLDS.clear();
        PLAYER_WORLDS.clear();
        RANGE_WORLDS.clear();
    }

    private static boolean allowed(boolean global, Map<String, Boolean> worlds) {
        String world = currentWorld();
        if (world != null) {
            Boolean specific = worlds.get(world);
            if (specific == null) {
                for (Map.Entry<String, Boolean> entry : worlds.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(world)) {
                        specific = entry.getValue();
                        break;
                    }
                }
            }
            if (specific != null) {
                return specific;
            }
        }
        return global;
    }

    private static String currentWorld() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        return player.level().dimension().identifier().toString();
    }
}
