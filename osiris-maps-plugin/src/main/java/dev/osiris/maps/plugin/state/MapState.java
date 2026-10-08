package dev.osiris.maps.plugin.state;

import dev.osiris.maps.plugin.OsirisMapsPlugin;
import dev.osiris.maps.plugin.net.Channels;
import dev.osiris.maps.plugin.net.PacketBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Estado en memoria. El disco se escribe aparte y el tick de rastreo solo corre si hay alguien marcado. */
public final class MapState {
    public enum Kind {
        MINIMAP,
        WORLDMAP
    }

    public enum Rule {
        WAYPOINTS,
        DEATHPOINTS,
        PLAYERS
    }

    private final OsirisMapsPlugin plugin;
    private final Map<UUID, EnumMap<Kind, Boolean>> display = new LinkedHashMap<>();
    private final EnumMap<Kind, Boolean> globalDisplay = new EnumMap<>(Kind.class);
    private final Map<UUID, EnumMap<Kind, Boolean>> bypass = new LinkedHashMap<>();
    private final Map<String, Waypoint> waypoints = new LinkedHashMap<>();
    private final Map<UUID, Track> tracks = new LinkedHashMap<>();
    private final EnumMap<Rule, Boolean> globalRules = new EnumMap<>(Rule.class);
    private final EnumMap<Rule, Map<String, Boolean>> worldRules = new EnumMap<>(Rule.class);
    private BukkitTask trackTask;
    private boolean saveQueued;
    /** null = usar el default de config.yml. false = puntos blancos. */
    private Boolean playerHeads;
    /** null = usar players.range de config.yml. true = full, false = distancia de render. */
    private Boolean rangeFull;
    private final Map<String, Boolean> rangeWorlds = new LinkedHashMap<>();

    public MapState(OsirisMapsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(StateFile.Snapshot snapshot) {
        this.display.clear();
        this.display.putAll(snapshot.display());
        this.globalDisplay.clear();
        this.globalDisplay.putAll(snapshot.globalDisplay());
        this.bypass.clear();
        this.bypass.putAll(snapshot.bypass());
        this.waypoints.clear();
        this.waypoints.putAll(snapshot.waypoints());
        this.tracks.clear();
        this.tracks.putAll(snapshot.tracks());
        this.globalRules.clear();
        this.globalRules.putAll(snapshot.globalRules());
        this.worldRules.clear();
        for (Map.Entry<Rule, Map<String, Boolean>> entry : snapshot.worldRules().entrySet()) {
            this.worldRules.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        this.playerHeads = snapshot.playerHeads();
        this.rangeFull = snapshot.rangeFull();
        this.rangeWorlds.clear();
        if (snapshot.rangeWorlds() != null) {
            this.rangeWorlds.putAll(snapshot.rangeWorlds());
        }
        if (!this.tracks.isEmpty()) {
            ensureTrackTask();
        }
    }

    public boolean visible(UUID player, Kind kind) {
        if (flag(this.bypass, player, kind)) {
            return true;
        }
        EnumMap<Kind, Boolean> forced = this.display.get(player);
        if (forced != null && forced.containsKey(kind)) {
            return Boolean.TRUE.equals(forced.get(kind));
        }
        return this.globalDisplay.getOrDefault(kind, this.plugin.settings().defaultDisplay(kind));
    }

    public void setDisplay(UUID player, Kind kind, boolean visible) {
        EnumMap<Kind, Boolean> forced = this.display.computeIfAbsent(player, ignored -> new EnumMap<>(Kind.class));
        forced.put(kind, visible);
        markDirty();
    }

    public void setGlobalDisplay(Kind kind, boolean visible) {
        this.globalDisplay.put(kind, visible);
        this.display.values().forEach(flags -> flags.remove(kind));
        this.display.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        markDirty();
    }

    public void broadcastDisplay() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendDisplay(player);
        }
    }

    public void setBypass(UUID player, Kind kind, boolean enabled) {
        EnumMap<Kind, Boolean> flags = this.bypass.computeIfAbsent(player, ignored -> new EnumMap<>(Kind.class));
        if (enabled) {
            flags.put(kind, true);
        } else {
            flags.remove(kind);
            if (flags.isEmpty()) {
                this.bypass.remove(player);
            }
        }
        markDirty();
    }

    /** Sin valor guardado por comando, se usa el default de config.yml. Un mundo concreto pisa la regla global. */
    public boolean allowed(Rule rule, String worldId) {
        Map<String, Boolean> worlds = this.worldRules.get(rule);
        if (worlds != null && worldId != null && worlds.containsKey(worldId)) {
            return Boolean.TRUE.equals(worlds.get(worldId));
        }
        Boolean global = this.globalRules.get(rule);
        if (global != null) {
            return global;
        }
        return this.plugin.settings().defaultRule(rule);
    }

    public void setRule(Rule rule, String worldId, boolean enabled) {
        if (worldId == null) {
            this.globalRules.put(rule, enabled);
        } else {
            this.worldRules.computeIfAbsent(rule, ignored -> new LinkedHashMap<>()).put(worldId, enabled);
        }
        markDirty();
    }

    public boolean waypointsGlobal() {
        return allowed(Rule.WAYPOINTS, null);
    }

    public boolean deathpointsGlobal() {
        return allowed(Rule.DEATHPOINTS, null);
    }

    public Map<String, Boolean> waypointWorlds() {
        return worldRules(Rule.WAYPOINTS);
    }

    public Map<String, Boolean> deathpointWorlds() {
        return worldRules(Rule.DEATHPOINTS);
    }

    public boolean playersGlobal() {
        return allowed(Rule.PLAYERS, null);
    }

    public Map<String, Boolean> playerWorlds() {
        return worldRules(Rule.PLAYERS);
    }

    public boolean playerHeads() {
        return this.playerHeads != null ? this.playerHeads : this.plugin.settings().playerHeads();
    }

    public void setPlayerHeads(boolean heads) {
        this.playerHeads = heads;
        markDirty();
    }

    public Boolean savedPlayerHeads() {
        return this.playerHeads;
    }

    /**
     * true = los jugadores se ven en todo el mapa y el servidor envia posiciones.
     * false = cada cliente usa su distancia de render y el servidor no lee posiciones.
     * Un mundo concreto pisa el valor global. Sin comando, manda config.yml.
     */
    public boolean rangeFull(String worldId) {
        if (worldId != null) {
            Boolean specific = this.rangeWorlds.get(worldId);
            if (specific == null) {
                for (Map.Entry<String, Boolean> entry : this.rangeWorlds.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(worldId)) {
                        specific = entry.getValue();
                        break;
                    }
                }
            }
            if (specific != null) {
                return specific;
            }
        }
        if (this.rangeFull != null) {
            return this.rangeFull;
        }
        return this.plugin.settings().defaultRangeFull();
    }

    public void setRange(String worldId, boolean full) {
        if (worldId == null) {
            this.rangeFull = full;
        } else {
            this.rangeWorlds.put(worldId, full);
        }
        markDirty();
    }

    public Map<String, Boolean> rangeWorlds() {
        return this.rangeWorlds;
    }

    public void putWaypoint(Waypoint waypoint) {
        this.waypoints.put(waypoint.id(), waypoint);
        markDirty();
    }

    public Waypoint removeWaypoint(String id) {
        Waypoint removed = this.waypoints.remove(id);
        if (removed != null) {
            markDirty();
        }
        return removed;
    }

    public Collection<Waypoint> waypoints() {
        return List.copyOf(this.waypoints.values());
    }

    public void putTrack(Track track) {
        this.tracks.put(track.playerId(), track);
        ensureTrackTask();
        markDirty();
    }

    public Track removeTrack(UUID player) {
        Track removed = this.tracks.remove(player);
        if (removed != null) {
            markDirty();
        }
        if (this.tracks.isEmpty() && this.trackTask != null) {
            this.trackTask.cancel();
            this.trackTask = null;
        }
        return removed;
    }

    public Track track(UUID player) {
        return this.tracks.get(player);
    }

    public Track trackByName(String name) {
        for (Track track : this.tracks.values()) {
            if (track.lastName().equalsIgnoreCase(name)) {
                return track;
            }
        }
        return null;
    }

    public void pushPlayer(Player player) {
        sendDisplay(player);
        sendRules(player);
        for (Waypoint waypoint : this.waypoints.values()) {
            plugin.messenger().send(player, Channels.ADD_WAYPOINT, waypoint.packet());
        }
        for (Track track : this.tracks.values()) {
            if (track.hasPosition()) {
                plugin.messenger().send(player, Channels.UPDATE_TRACKING, track.packet());
            }
        }
        if (plugin.markers() != null) {
            plugin.markers().send(player);
        }
    }

    public void sendRules(Player player) {
        plugin.messenger().send(player, Channels.MAP_RULES, rulesPacket());
    }

    public void broadcastRules() {
        byte[] body = rulesPacket();
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.messenger().send(player, Channels.MAP_RULES, body);
        }
    }

    public void sendDisplay(Player player) {
        plugin.messenger().send(player, Channels.DISPLAY_MINIMAP, PacketBuf.display(visible(player.getUniqueId(), Kind.MINIMAP)));
        plugin.messenger().send(player, Channels.DISPLAY_WORLDMAP, PacketBuf.display(visible(player.getUniqueId(), Kind.WORLDMAP)));
    }

    public void broadcastWaypoint(Waypoint waypoint) {
        byte[] body = waypoint.packet();
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.messenger().send(player, Channels.ADD_WAYPOINT, body);
        }
    }

    public void broadcastRemove(String id) {
        byte[] body = PacketBuf.remove(id);
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.messenger().send(player, Channels.REMOVE_WAYPOINT, body);
        }
    }

    public void shutdown() {
        if (this.trackTask != null) {
            this.trackTask.cancel();
            this.trackTask = null;
        }
        StateFile.write(plugin, snapshot());
    }

    private void ensureTrackTask() {
        if (this.trackTask != null) {
            return;
        }
        long ticks = plugin.settings().trackingTicks();
        this.trackTask = Bukkit.getScheduler().runTaskTimer(plugin, this::trackTick, ticks, ticks);
    }

    public void restartTrackTask() {
        if (this.trackTask != null) {
            this.trackTask.cancel();
            this.trackTask = null;
        }
        if (!this.tracks.isEmpty()) {
            ensureTrackTask();
        }
    }

    private void trackTick() {
        if (this.tracks.isEmpty()) {
            return;
        }
        List<byte[]> packets = new ArrayList<>(this.tracks.size());
        for (Track track : this.tracks.values()) {
            Player player = Bukkit.getPlayer(track.playerId());
            if (player == null) {
                continue;
            }
            Location location = player.getLocation();
            World world = location.getWorld();
            if (world == null) {
                continue;
            }
            String worldId = world.getKey().asString();
            if (!track.moved(worldId, location.getX(), location.getY(), location.getZ(), plugin.settings().minMoveSquared())) {
                continue;
            }
            track.remember(player.getName(), worldId, location.getX(), location.getY(), location.getZ());
            packets.add(track.packet());
        }
        if (packets.isEmpty()) {
            return;
        }
        List<Player> listeners = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getListeningPluginChannels().contains(Channels.UPDATE_TRACKING)) {
                listeners.add(player);
            }
        }
        if (listeners.isEmpty()) {
            return;
        }
        for (byte[] body : packets) {
            for (Player listener : listeners) {
                plugin.messenger().sendRaw(listener, Channels.UPDATE_TRACKING, body);
            }
        }
    }

    private void markDirty() {
        if (this.saveQueued) {
            return;
        }
        this.saveQueued = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.saveQueued = false;
            StateFile.Snapshot copy = snapshot();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> StateFile.write(plugin, copy));
        }, 20L);
    }

    private StateFile.Snapshot snapshot() {
        return new StateFile.Snapshot(
                copyFlags(this.display),
                copyFlags(this.bypass),
                new LinkedHashMap<>(this.waypoints),
                new LinkedHashMap<>(this.tracks),
                new EnumMap<>(this.globalRules),
                copyWorldRules(),
                new EnumMap<>(this.globalDisplay),
                this.playerHeads,
                this.rangeFull,
                new LinkedHashMap<>(this.rangeWorlds)
        );
    }

    private EnumMap<Rule, Map<String, Boolean>> copyWorldRules() {
        EnumMap<Rule, Map<String, Boolean>> copy = new EnumMap<>(Rule.class);
        for (Map.Entry<Rule, Map<String, Boolean>> entry : this.worldRules.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }

    private byte[] rulesPacket() {
        return PacketBuf.rules(
                waypointsGlobal(), waypointWorlds(),
                deathpointsGlobal(), deathpointWorlds(),
                playersGlobal(), playerWorlds(),
                rangeFull(null), this.rangeWorlds);
    }

    private Map<String, Boolean> worldRules(Rule rule) {
        Map<String, Boolean> worlds = this.worldRules.get(rule);
        return worlds == null ? Map.of() : worlds;
    }

    private static Map<UUID, EnumMap<Kind, Boolean>> copyFlags(Map<UUID, EnumMap<Kind, Boolean>> source) {
        Map<UUID, EnumMap<Kind, Boolean>> copy = new LinkedHashMap<>();
        for (Map.Entry<UUID, EnumMap<Kind, Boolean>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new EnumMap<>(entry.getValue()));
        }
        return copy;
    }

    private static boolean flag(Map<UUID, EnumMap<Kind, Boolean>> source, UUID player, Kind kind) {
        EnumMap<Kind, Boolean> flags = source.get(player);
        return flags != null && Boolean.TRUE.equals(flags.get(kind));
    }

    public record Waypoint(String id, String world, double x, double y, double z, int color, String name) {
        public byte[] packet() {
            return PacketBuf.point(id, world, x, y, z, color, name);
        }
    }

    public static final class Track {
        private final UUID playerId;
        private final int color;
        private final String label;
        private String lastName;
        private String world = "";
        private double x;
        private double y;
        private double z;
        private boolean positioned;

        public Track(UUID playerId, String lastName, int color, String label) {
            this.playerId = playerId;
            this.lastName = lastName;
            this.color = color;
            this.label = label;
        }

        public UUID playerId() {
            return this.playerId;
        }

        public String lastName() {
            return this.lastName;
        }

        public int color() {
            return this.color;
        }

        public String label() {
            return this.label;
        }

        public boolean hasPosition() {
            return this.positioned;
        }

        public boolean moved(String worldId, double nextX, double nextY, double nextZ, double minSquared) {
            if (!this.positioned || !worldId.equals(this.world)) {
                return true;
            }
            double dx = nextX - this.x;
            double dy = nextY - this.y;
            double dz = nextZ - this.z;
            return dx * dx + dy * dy + dz * dz >= minSquared;
        }

        public void remember(String name, String worldId, double nextX, double nextY, double nextZ) {
            this.lastName = name;
            this.world = worldId;
            this.x = nextX;
            this.y = nextY;
            this.z = nextZ;
            this.positioned = true;
        }

        public byte[] packet() {
            return PacketBuf.point(PacketBuf.trackId(this.playerId), this.world, this.x, this.y, this.z, this.color, this.label);
        }

        public String fileLine() {
            return this.playerId + "\t" + this.color + "\t" + this.lastName + "\t" + this.label;
        }

        public static Track parse(String line) {
            String[] parts = line.split("\t", 4);
            if (parts.length < 4) {
                return null;
            }
            return new Track(UUID.fromString(parts[0]), parts[2], Integer.parseInt(parts[1]), parts[3]);
        }
    }

    public static String worldId(World world) {
        return world.getKey().asString().toLowerCase(Locale.ROOT);
    }
}
