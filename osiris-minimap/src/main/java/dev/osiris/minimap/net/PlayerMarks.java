package dev.osiris.minimap.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.mcworld.MinimapClientWorldData;
import xaero.common.minimap.mcworld.MinimapClientWorldDataHelper;
import xaero.common.server.radar.tracker.SyncedTrackedPlayer;
import xaero.hud.minimap.player.tracker.synced.ClientSyncedTrackedPlayerManager;

/**
 * Los otros jugadores entran al tracker de Xaero. Con cabezas se deja el icono de Xaero.
 * Sin cabezas se dibuja el punto del radar de entidades. Tab muestra la cabeza mientras está pulsado.
 * En default el cliente usa a los jugadores que Minecraft ya cargó. En full usa la lista del servidor.
 */
public final class PlayerMarks {
    private static boolean heads;
    private static boolean pending;
    private static boolean showing;
    private static String world = "";
    private static String renderedWorld = "";
    private static List<RemoteWaypoints.RadarDot> dots = List.of();
    private static final Map<UUID, Float> YAW = new HashMap<>();
    private static UUID rendering;

    private PlayerMarks() {
    }

    public static boolean heads() {
        return heads;
    }

    public static void restore(boolean value) {
        heads = value;
    }

    /** Cabeza fija por comando, o la cabeza temporal mientras Tab (lista de jugadores) está pulsado. */
    public static boolean showHeads() {
        return heads || tabDown();
    }

    private static boolean tabDown() {
        Minecraft minecraft = Minecraft.getInstance();
        var screen = minecraft.screen;
        minecraft.screen = null;
        try {
            return minecraft.options.keyPlayerList.isDown();
        } finally {
            minecraft.screen = screen;
        }
    }

    /** En default solo cuenta un jugador que Minecraft ya tiene cargado. Fuera de ese rango no hay icono. */
    public static boolean loaded(UUID id) {
        if (id == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || id.equals(minecraft.player.getUUID())) {
            return false;
        }
        var player = minecraft.level.getPlayerByUUID(id);
        return player != null && player.isAlive();
    }

    public static void apply(String worldId, boolean showHeads, boolean serverList, List<RemoteWaypoints.RadarDot> next) {
        world = worldId == null ? "" : worldId;
        heads = showHeads;
        boolean useServer = serverList && MapRules.fullTracking();
        if (!useServer) {
            dots = List.of();
            pending = false;
            if (!MapRules.players()) {
                wipe();
                return;
            }
            syncLocal();
            return;
        }
        dots = next == null ? List.of() : List.copyOf(next);
        pending = true;
        if (!viewing(world)) {
            return;
        }
        if (!MapRules.players()) {
            pending = false;
            wipe();
            return;
        }
        flush();
    }

    public static void tick() {
        if (!MapRules.players()) {
            pending = false;
            if (showing) {
                wipe();
            }
            return;
        }
        if (!MapRules.fullTracking()) {
            syncLocal();
            return;
        }
        if (pending && viewing(world)) {
            flush();
            return;
        }
        String here = currentWorld();
        if (showing && here != null && !here.equalsIgnoreCase(renderedWorld)) {
            wipe();
        }
    }

    public static void clear() {
        heads = false;
        pending = false;
        showing = false;
        world = "";
        renderedWorld = "";
        dots = List.of();
        YAW.clear();
        rendering = null;
        ClientSyncedTrackedPlayerManager manager = manager();
        if (manager != null) {
            manager.reset();
        }
    }

    public static void rendering(UUID player) {
        rendering = player;
    }

    public static float arrowRotation(UUID player) {
        UUID id = player != null ? player : rendering;
        float yaw = id == null ? 0.0F : YAW.getOrDefault(id, 0.0F);
        return yaw - 180.0F;
    }

    private static void flush() {
        ClientSyncedTrackedPlayerManager manager = manager();
        if (manager == null) {
            return;
        }
        unlockTracker();
        ResourceKey<Level> dimension = dimension(world);
        UUID self = selfId();
        Set<UUID> keep = new HashSet<>();
        YAW.clear();
        for (RemoteWaypoints.RadarDot dot : dots) {
            UUID id = id(dot.id());
            if (id == null || id.equals(self)) {
                continue;
            }
            keep.add(id);
            YAW.put(id, dot.yaw());
            manager.update(id, dot.x(), dot.y(), dot.z(), dimension);
        }
        dropMissing(manager, keep);
        pending = false;
        showing = true;
        renderedWorld = currentWorld() == null ? "" : currentWorld();
    }

    private static void syncLocal() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        ClientSyncedTrackedPlayerManager manager = manager();
        if (manager == null) {
            return;
        }
        unlockTracker();
        ResourceKey<Level> dimension = minecraft.level.dimension();
        UUID self = minecraft.player.getUUID();
        Set<UUID> keep = new HashSet<>();
        YAW.clear();
        for (var player : minecraft.level.players()) {
            if (!player.isAlive() || player.getUUID().equals(self)) {
                continue;
            }
            UUID id = player.getUUID();
            keep.add(id);
            YAW.put(id, player.getYRot());
            manager.update(id, player.getX(), player.getY(), player.getZ(), dimension);
        }
        dropMissing(manager, keep);
        showing = true;
        renderedWorld = dimension.identifier().toString();
    }

    private static void wipe() {
        YAW.clear();
        rendering = null;
        if (!pending) {
            dots = List.of();
        }
        ClientSyncedTrackedPlayerManager manager = manager();
        if (manager == null) {
            return;
        }
        manager.reset();
        showing = false;
    }

    private static void dropMissing(ClientSyncedTrackedPlayerManager manager, Set<UUID> keep) {
        List<UUID> stale = new ArrayList<>();
        for (SyncedTrackedPlayer player : manager.getPlayers()) {
            if (!keep.contains(player.getId())) {
                stale.add(player.getId());
            }
        }
        for (UUID id : stale) {
            manager.remove(id);
        }
    }

    private static String currentWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return minecraft.level.dimension().identifier().toString();
    }

    private static boolean viewing(String worldId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        if (worldId == null || worldId.isBlank()) {
            return true;
        }
        return worldId.equalsIgnoreCase(minecraft.level.dimension().identifier().toString());
    }

    private static UUID selfId() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : minecraft.player.getUUID();
    }

    private static void unlockTracker() {
        MinimapClientWorldData data = MinimapClientWorldDataHelper.getCurrentWorldData();
        if (data != null && data.serverLevelId == null) {
            data.serverLevelId = 0;
        }
    }

    private static ClientSyncedTrackedPlayerManager manager() {
        XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
        if (session == null) {
            return null;
        }
        MinimapProcessor processor = session.getMinimapProcessor();
        if (processor == null) {
            return null;
        }
        return processor.getSyncedTrackedPlayerManager();
    }

    private static UUID id(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.startsWith("radar:") ? raw.substring(6) : raw;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static ResourceKey<Level> dimension(String worldId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && worldId.equalsIgnoreCase(minecraft.level.dimension().identifier().toString())) {
            return minecraft.level.dimension();
        }
        Identifier identifier = Identifier.parse(worldId.isBlank() ? "minecraft:overworld" : worldId);
        return ResourceKey.create(Registries.DIMENSION, identifier);
    }
}
