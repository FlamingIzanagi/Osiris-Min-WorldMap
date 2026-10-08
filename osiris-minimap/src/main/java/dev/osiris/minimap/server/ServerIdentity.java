package dev.osiris.minimap.server;

import dev.osiris.minimap.OsirisMinimapClient;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Huella del backend actual, calculada solo con el login de vanilla.
 * Una redirección de network abre otra sesión de juego con la misma IP del proxy.
 * Viajar al Nether o al End no cambia la huella: la semilla y la lista de dimensiones son del backend.
 */
public final class ServerIdentity {
    private static final Object LOCK = new Object();
    private static final int MAX_SEEN = 128;
    private static final int REFRESH_TICKS = 200;
    private static final ArrayDeque<String> SEEN = new ArrayDeque<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    private static volatile String current;
    private static boolean active;
    private static long seed;
    private static int chunkRadius;
    private static int simulationDistance;
    private static int maxPlayers;
    private static boolean hardcore;
    private static String levels = "0";
    private static int pendingTicks;
    private static volatile boolean failureLogged;

    private ServerIdentity() {
    }

    @FunctionalInterface
    public interface Listener {
        /** @return false si la sesión de Xaero todavía no existe y hay que reintentar */
        boolean onBackendChanged();
    }

    public static void listen(Listener listener) {
        Objects.requireNonNull(listener, "listener");
        synchronized (LOCK) {
            LISTENERS.add(listener);
        }
    }

    public static String current() {
        return current;
    }

    public static void onLogin(ClientboundLoginPacket packet) {
        if (localWorld()) {
            stopRefresh();
            return;
        }
        CommonPlayerSpawnInfo spawn = packet.commonPlayerSpawnInfo();
        accept(
                spawn.seed(),
                packet.chunkRadius(),
                packet.simulationDistance(),
                packet.maxPlayers(),
                packet.hardcore(),
                levelsToken(packet.levels())
        );
    }

    /**
     * El Nether, el End y la muerte repiten la misma semilla.
     * Un proxy viejo que cambia de backend solo con respawn trae otra semilla.
     */
    public static void onRespawn(ClientboundRespawnPacket packet) {
        if (localWorld()) {
            return;
        }
        long nextSeed = packet.commonPlayerSpawnInfo().seed();
        int radius;
        int simulation;
        int players;
        boolean nextHardcore;
        String levelToken;
        synchronized (LOCK) {
            if (active && nextSeed == seed) {
                return;
            }
            radius = chunkRadius;
            simulation = simulationDistance;
            players = maxPlayers;
            nextHardcore = hardcore;
            levelToken = levels;
        }
        accept(nextSeed, radius, simulation, players, nextHardcore, levelToken);
    }

    public static void onDisconnect() {
        stopRefresh();
    }

    /** Un mundo local no es un backend de la network: no hay carpeta extra ni refresco. */
    public static boolean localWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.isSingleplayer() || minecraft.hasSingleplayerServer();
    }

    public static void tick() {
        List<Listener> snapshot = List.of();
        String published = null;
        boolean refresh = false;
        synchronized (LOCK) {
            if (pendingTicks <= 0) {
                return;
            }
            pendingTicks--;
            refresh = true;
            published = current;
            snapshot = List.copyOf(LISTENERS);
        }
        if (!refresh) {
            return;
        }
        boolean allReady = true;
        boolean failed = false;
        for (Listener listener : snapshot) {
            try {
                if (!listener.onBackendChanged()) {
                    allReady = false;
                }
            } catch (Throwable error) {
                allReady = false;
                failed = true;
                if (!failureLogged) {
                    failureLogged = true;
                    OsirisMinimapClient.LOGGER.warn("No se pudo cambiar el mapa de backend", error);
                }
            }
        }
        if (failed) {
            stopRefresh();
            return;
        }
        if (!allReady) {
            return;
        }
        synchronized (LOCK) {
            if (Objects.equals(current, published)) {
                pendingTicks = 0;
            }
        }
    }

    /**
     * Inserta la huella después de {@code Multiplayer_<ip>} y conserva el resto de la ruta.
     * La IP del proxy no cambia entre backends; la huella sí.
     */
    public static String folder(String worldId) {
        if (worldId == null || !worldId.startsWith("Multiplayer_")) {
            return worldId;
        }
        int slash = worldId.indexOf('/');
        String base = slash < 0 ? worldId : worldId.substring(0, slash);
        String rest = slash < 0 ? "" : worldId.substring(slash + 1);
        if (!rest.isEmpty()) {
            int next = rest.indexOf('/');
            String first = next < 0 ? rest : rest.substring(0, next);
            if (isSubServerNode(first)) {
                rest = next < 0 ? "" : rest.substring(next + 1);
            }
        }
        String sub = current;
        if (sub == null) {
            return rest.isEmpty() ? base : base + "/" + rest;
        }
        String combined = rest.isEmpty() ? base + "/" + sub : base + "/" + sub + "/" + rest;
        return combined.equals(worldId) ? worldId : combined;
    }

    public static boolean isSubServerNode(String node) {
        if (node == null) {
            return false;
        }
        synchronized (LOCK) {
            return SEEN.contains(node);
        }
    }

    private static void accept(
            long nextSeed,
            int radius,
            int simulation,
            int players,
            boolean nextHardcore,
            String levelToken
    ) {
        String published;
        boolean changed;
        synchronized (LOCK) {
            active = true;
            seed = nextSeed;
            chunkRadius = radius;
            simulationDistance = simulation;
            maxPlayers = players;
            hardcore = nextHardcore;
            levels = levelToken == null ? "0" : levelToken;
            published = tokenLocked();
            changed = assignLocked(published);
        }
        if (changed) {
            OsirisMinimapClient.LOGGER.info("Carpeta de mapa del backend: {}", published);
        }
    }

    private static boolean assignLocked(String published) {
        if (Objects.equals(current, published)) {
            return false;
        }
        current = published;
        rememberLocked(published);
        pendingTicks = REFRESH_TICKS;
        failureLogged = false;
        return true;
    }

    private static String tokenLocked() {
        return "s" + Long.toUnsignedString(seed, 16)
                + "-r" + chunkRadius
                + "-d" + simulationDistance
                + "-m" + maxPlayers
                + "-l" + levels
                + "-h" + (hardcore ? '1' : '0');
    }

    private static String levelsToken(Set<ResourceKey<Level>> levels) {
        if (levels == null || levels.isEmpty()) {
            return "0";
        }
        String[] ids = new String[levels.size()];
        int index = 0;
        for (ResourceKey<Level> level : levels) {
            ids[index++] = level.identifier().toString();
        }
        Arrays.sort(ids);
        int hash = 1;
        for (String id : ids) {
            hash = 31 * hash + id.hashCode();
        }
        return Integer.toHexString(hash);
    }

    private static void stopRefresh() {
        synchronized (LOCK) {
            pendingTicks = 0;
        }
    }

    private static void rememberLocked(String sub) {
        if (sub == null) {
            return;
        }
        SEEN.remove(sub);
        SEEN.addLast(sub);
        while (SEEN.size() > MAX_SEEN) {
            SEEN.removeFirst();
        }
    }
}
