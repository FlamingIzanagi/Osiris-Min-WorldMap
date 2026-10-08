package dev.osiris.minimap.net;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointVisibilityType;
import xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypointManager;
import xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

/**
 * Waypoints que manda el servidor. Viven en el gestor de terceros de Xaero, no en un set del jugador.
 * El rastreo interpola en cada frame; un cambio de dimensión salta de mapa sin arrastrar la posición vieja.
 */
public final class RemoteWaypoints {
    public static final Identifier ORIGIN = Identifier.fromNamespaceAndPath("osirismaps", "server");
    private static final long SLIDE_NANOS = 200_000_000L;

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static final Map<String, Waypoint> HANDLES = new LinkedHashMap<>();
    private static final Map<Waypoint, String> OWNED = new IdentityHashMap<>();

    private static boolean dirty = true;
    private static ResourceKey<Level> lastDimension;

    private RemoteWaypoints() {
    }

    public static void add(String id, String world, double x, double y, double z, int color, String name, boolean tracking) {
        Entry entry = ENTRIES.get(id);
        if (entry == null) {
            entry = new Entry(id);
            ENTRIES.put(id, entry);
            entry.snap(world, x, y, z);
        } else if (!world.equals(entry.world)) {
            forgetHandle(id, entry.world);
            entry.snap(world, x, y, z);
        } else if (tracking) {
            entry.retarget(x, y, z);
        } else {
            entry.snap(world, x, y, z);
        }
        entry.world = world;
        entry.color = color;
        entry.name = name == null || name.isEmpty() ? id : name;
        entry.tracking = tracking;
        entry.placed = false;
        dirty = true;
    }

    public static void track(String id, String world, double x, double y, double z, int color, String name) {
        add(id, world, x, y, z, color, name, true);
    }

    /** La lista es el conjunto completo de jugadores visibles. El icono lo dibuja el tracker de Xaero. */
    public static void syncRadar(String world, boolean heads, boolean full, List<RadarDot> dots) {
        List<String> drop = new ArrayList<>();
        for (String id : ENTRIES.keySet()) {
            if (id.startsWith("radar:")) {
                drop.add(id);
            }
        }
        for (String id : drop) {
            remove(id);
        }
        PlayerMarks.apply(world, heads, full, dots);
    }

    public record RadarDot(String id, double x, double y, double z, float yaw, String name) {
    }

    public static void remove(String id) {
        Entry entry = ENTRIES.remove(id);
        if (entry != null) {
            forgetHandle(id, entry.world);
        }
        dirty = true;
    }

    public static void clear() {
        for (Entry entry : ENTRIES.values()) {
            forgetHandle(entry.id, entry.world);
        }
        ENTRIES.clear();
        HANDLES.clear();
        OWNED.clear();
        dirty = false;
        PlayerMarks.clear();
        lastDimension = null;
        MinimapSession session = session();
        if (session == null) {
            return;
        }
        for (MinimapWorldRootContainer root : session.getWorldManager().getRootContainers()) {
            clearOrigin(root);
            for (MinimapWorld world : root.getAllWorldsIterable()) {
                clearOrigin(world.getContainer());
            }
        }
    }

    public static boolean locked(Waypoint waypoint) {
        if (waypoint == null) {
            return false;
        }
        Identifier origin = waypoint.getThirdPartyOrigin();
        return origin != null && ORIGIN.getNamespace().equals(origin.getNamespace());
    }

    public static void tick() {
        PlayerMarks.tick();
        Minecraft minecraft = Minecraft.getInstance();
        ResourceKey<Level> dimension = minecraft.player == null ? null : minecraft.player.level().dimension();
        if (dimension != lastDimension) {
            lastDimension = dimension;
            dirty = true;
        }
        if (!dirty) {
            return;
        }
        boolean pending = false;
        for (Entry entry : ENTRIES.values()) {
            if (!place(entry)) {
                pending = true;
            }
        }
        dirty = pending;
    }

    /** Alinea el bloque entero antes de que Xaero lea el waypoint. La fracción se suma al dibujar. */
    public static void applyFrame() {
        for (Entry entry : ENTRIES.values()) {
            if (!entry.tracking) {
                continue;
            }
            Waypoint waypoint = HANDLES.get(entry.id);
            if (waypoint == null) {
                continue;
            }
            waypoint.setX((int) Math.round(entry.displayX()));
            waypoint.setY((int) Math.round(entry.displayY()));
            waypoint.setZ((int) Math.round(entry.displayZ()));
        }
    }

    public static double slideX(Waypoint waypoint, double stored) {
        return slide(waypoint, stored, true);
    }

    public static double slideZ(Waypoint waypoint, double stored) {
        return slide(waypoint, stored, false);
    }

    private static double slide(Waypoint waypoint, double stored, boolean xAxis) {
        String id = OWNED.get(waypoint);
        if (id == null) {
            return stored;
        }
        Entry entry = ENTRIES.get(id);
        if (entry == null || !entry.tracking) {
            return stored;
        }
        double value = xAxis ? entry.displayX() : entry.displayZ();
        return stored + (value - Math.round(value));
    }

    private static boolean place(Entry entry) {
        MinimapSession session = session();
        if (session == null) {
            return false;
        }
        ThirdPartyWaypointManager manager = managerFor(session, entry.world);
        if (manager == null) {
            return true;
        }
        ThirdPartyWaypoints group = manager.get(ORIGIN);
        Waypoint waypoint = group.get(entry.id);
        int x = (int) Math.round(entry.tracking ? entry.displayX() : entry.toX);
        int y = (int) Math.round(entry.tracking ? entry.displayY() : entry.toY);
        int z = (int) Math.round(entry.tracking ? entry.displayZ() : entry.toZ);
        if (waypoint == null) {
            waypoint = new Waypoint(x, y, z, entry.name, initials(entry.name), color(entry.color));
            waypoint.setYIncluded(true);
            waypoint.setTemporary(false);
            waypoint.setDisabled(false);
            waypoint.setVisibility(WaypointVisibilityType.GLOBAL);
            group.add(entry.id, waypoint);
        } else if (!entry.tracking) {
            waypoint.setX(x);
            waypoint.setY(y);
            waypoint.setZ(z);
        }
        waypoint.setName(entry.name);
        waypoint.setColor(entry.color);
        waypoint.setThirdPartyOrigin(ORIGIN);
        waypoint.setDisabled(false);
        HANDLES.put(entry.id, waypoint);
        OWNED.put(waypoint, entry.id);
        entry.placed = true;
        return true;
    }

    private static void forgetHandle(String id, String world) {
        Waypoint waypoint = HANDLES.remove(id);
        if (waypoint != null) {
            OWNED.remove(waypoint);
        }
        MinimapSession session = session();
        if (session == null) {
            return;
        }
        ThirdPartyWaypointManager manager = managerFor(session, world);
        if (manager == null) {
            return;
        }
        ThirdPartyWaypoints group = manager.get(ORIGIN);
        group.remove(id);
    }

    private static ThirdPartyWaypointManager managerFor(MinimapSession session, String worldId) {
        MinimapWorldManager worlds = session.getWorldManager();
        MinimapWorld current = worlds.getCurrentWorld();
        if (current != null && sameWorld(current.getDimId(), worldId)) {
            ThirdPartyWaypointManager manager = current.getContainer().getThirdPartyWaypointManager();
            if (manager != null) {
                return manager;
            }
        }
        for (MinimapWorldRootContainer root : worlds.getRootContainers()) {
            for (MinimapWorld world : root.getAllWorldsIterable()) {
                if (!sameWorld(world.getDimId(), worldId)) {
                    continue;
                }
                ThirdPartyWaypointManager manager = world.getContainer().getThirdPartyWaypointManager();
                if (manager != null) {
                    return manager;
                }
            }
        }
        return null;
    }

    private static boolean sameWorld(ResourceKey<Level> dimension, String worldId) {
        if (dimension == null || worldId == null) {
            return false;
        }
        if (worldId.equalsIgnoreCase(dimension.identifier().toString())) {
            return true;
        }
        if (worldId.equalsIgnoreCase(dimension.identifier().getPath())) {
            return true;
        }
        MinimapSession session = session();
        if (session == null) {
            return false;
        }
        String directory = session.getDimensionHelper().getDimensionDirectoryName(dimension);
        return worldId.equalsIgnoreCase(directory);
    }

    private static void clearOrigin(xaero.hud.minimap.world.container.MinimapWorldContainer container) {
        if (container == null) {
            return;
        }
        ThirdPartyWaypointManager manager = container.getThirdPartyWaypointManager();
        if (manager != null) {
            manager.clearOrigin(ORIGIN);
        }
    }

    private static MinimapSession session() {
        XaeroMinimapSession current = XaeroMinimapSession.getCurrentSession();
        if (current == null) {
            return null;
        }
        MinimapProcessor processor = current.getMinimapProcessor();
        if (processor == null) {
            return null;
        }
        return processor.getSession();
    }

    private static WaypointColor color(int index) {
        WaypointColor[] values = WaypointColor.values();
        int safe = Math.floorMod(index, values.length);
        return WaypointColor.fromIndex(safe);
    }

    private static String initials(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        int count = Math.min(2, trimmed.length());
        return trimmed.substring(0, count);
    }

    private static final class Entry {
        private final String id;
        private String world = "";
        private String name = "";
        private int color;
        private boolean tracking;
        private boolean placed;
        private double fromX;
        private double fromY;
        private double fromZ;
        private double toX;
        private double toY;
        private double toZ;
        private long startNanos;

        private Entry(String id) {
            this.id = id;
        }

        private void snap(String world, double x, double y, double z) {
            this.world = world;
            this.fromX = this.toX = x;
            this.fromY = this.toY = y;
            this.fromZ = this.toZ = z;
            this.startNanos = System.nanoTime();
        }

        private void retarget(double x, double y, double z) {
            this.fromX = displayX();
            this.fromY = displayY();
            this.fromZ = displayZ();
            this.toX = x;
            this.toY = y;
            this.toZ = z;
            this.startNanos = System.nanoTime();
        }

        private double displayX() {
            return mix(this.fromX, this.toX);
        }

        private double displayY() {
            return mix(this.fromY, this.toY);
        }

        private double displayZ() {
            return mix(this.fromZ, this.toZ);
        }

        private double mix(double from, double to) {
            double t = (System.nanoTime() - this.startNanos) / (double) SLIDE_NANOS;
            if (t <= 0.0) {
                return from;
            }
            if (t >= 1.0) {
                return to;
            }
            t = t * t * (3.0 - 2.0 * t);
            return from + (to - from) * t;
        }
    }
}
