package dev.osiris.maps.plugin.net;

/** Mismos canales que registra el cliente Fabric. El cuerpo lo escribe {@link PacketBuf}. */
public final class Channels {
    public static final String DISPLAY_MINIMAP = "osirismaps:display_minimap";
    public static final String DISPLAY_WORLDMAP = "osirismaps:display_worldmap";
    public static final String ADD_WAYPOINT = "osirismaps:add_waypoint";
    public static final String REMOVE_WAYPOINT = "osirismaps:remove_waypoint";
    public static final String UPDATE_TRACKING = "osirismaps:update_tracking";
    public static final String MAP_RULES = "osirismaps:map_rules";
    public static final String PLAYERS = "osirismaps:players";
    /** Cliente -> servidor: la revisión que ya tiene guardada. */
    public static final String HELLO = "osirismaps:hello";
    /** Servidor -> cliente: vacía los waypoints estáticos antes de una copia completa. */
    public static final String SNAPSHOT = "osirismaps:snapshot";
    /** Servidor -> cliente: número de revisión, después de aplicar los cambios. */
    public static final String REVISION = "osirismaps:revision";

    public static final String[] ALL = {
            DISPLAY_MINIMAP,
            DISPLAY_WORLDMAP,
            ADD_WAYPOINT,
            REMOVE_WAYPOINT,
            UPDATE_TRACKING,
            MAP_RULES,
            PLAYERS,
            HELLO,
            SNAPSHOT,
            REVISION
    };

    private Channels() {
    }
}
