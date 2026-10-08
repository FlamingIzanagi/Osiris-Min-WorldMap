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

    public static final String[] ALL = {
            DISPLAY_MINIMAP,
            DISPLAY_WORLDMAP,
            ADD_WAYPOINT,
            REMOVE_WAYPOINT,
            UPDATE_TRACKING,
            MAP_RULES,
            PLAYERS
    };

    private Channels() {
    }
}
