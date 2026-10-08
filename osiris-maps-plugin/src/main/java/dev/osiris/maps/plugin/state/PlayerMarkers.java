package dev.osiris.maps.plugin.state;

import dev.osiris.maps.plugin.OsirisMapsPlugin;
import dev.osiris.maps.plugin.net.Channels;
import dev.osiris.maps.plugin.net.PacketBuf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Iconos de los demas jugadores.
 * En default el servidor no lee posiciones: cada cliente dibuja a quien ya tiene cargado.
 * En full se arma una lista por mundo y se manda el mismo paquete a quien este ahi, solo si cambio.
 */
public final class PlayerMarkers {
    private final OsirisMapsPlugin plugin;
    private final Map<UUID, String> lastSignature = new HashMap<>();
    private BukkitTask task;

    public PlayerMarkers(OsirisMapsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        long ticks = this.plugin.settings().playersTicks();
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, ticks, ticks);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
        this.lastSignature.clear();
    }

    public void refresh() {
        this.lastSignature.clear();
        tick();
    }

    public void forget(UUID player) {
        this.lastSignature.remove(player);
    }

    public void send(Player viewer) {
        this.lastSignature.remove(viewer.getUniqueId());
        deliver(viewer, null);
    }

    private void tick() {
        Map<String, Frame> frames = new HashMap<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            deliver(viewer, frames);
        }
    }

    private void deliver(Player viewer, Map<String, Frame> frames) {
        if (!viewer.getListeningPluginChannels().contains(Channels.PLAYERS)) {
            return;
        }
        String worldId = MapState.worldId(viewer.getWorld());
        Frame frame = frames == null ? null : frames.get(worldId);
        if (frame == null) {
            frame = frame(viewer.getWorld(), worldId);
            if (frames != null) {
                frames.put(worldId, frame);
            }
        }
        if (frame.signature.equals(this.lastSignature.get(viewer.getUniqueId()))) {
            return;
        }
        this.lastSignature.put(viewer.getUniqueId(), frame.signature);
        this.plugin.messenger().send(viewer, Channels.PLAYERS, frame.packet());
    }

    private Frame frame(World world, String worldId) {
        boolean heads = this.plugin.state().playerHeads();
        int color = this.plugin.settings().playersColor();
        boolean visible = this.plugin.state().allowed(MapState.Rule.PLAYERS, worldId);
        boolean full = visible && this.plugin.state().rangeFull(worldId);
        List<Dot> dots = full ? collect(world) : List.of();
        if (dots.size() <= 1) {
            dots = List.of();
        }
        String mode = !visible ? "off" : full ? "full" : "local";
        return new Frame(signature(worldId, color, heads, mode, dots), heads, full, worldId, color, dots);
    }

    private static List<Dot> collect(World world) {
        List<Dot> dots = new ArrayList<>();
        for (Player other : world.getPlayers()) {
            if (other.isDead()) {
                continue;
            }
            Location location = other.getLocation();
            dots.add(new Dot(
                    other.getUniqueId().toString(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    location.getYaw(),
                    other.getName()));
        }
        dots.sort(Comparator.comparing(Dot::id));
        return dots;
    }

    private static String signature(String worldId, int color, boolean heads, String mode, List<Dot> dots) {
        StringBuilder text = new StringBuilder(worldId)
                .append('#')
                .append(color)
                .append(heads ? "#heads" : "#arrows")
                .append('#')
                .append(mode);
        for (Dot dot : dots) {
            text.append('|').append(dot.id).append('@')
                    .append(dot.name).append('@')
                    .append(Math.round(dot.x * 20.0)).append(',')
                    .append(Math.round(dot.y * 20.0)).append(',')
                    .append(Math.round(dot.z * 20.0)).append(',')
                    .append(Math.round(dot.yaw));
        }
        return text.toString();
    }

    public record Dot(String id, double x, double y, double z, float yaw, String name) {
    }

    private static final class Frame {
        private final String signature;
        private final boolean heads;
        private final boolean full;
        private final String worldId;
        private final int color;
        private final List<Dot> dots;
        private byte[] packet;

        private Frame(String signature, boolean heads, boolean full, String worldId, int color, List<Dot> dots) {
            this.signature = signature;
            this.heads = heads;
            this.full = full;
            this.worldId = worldId;
            this.color = color;
            this.dots = dots;
        }

        private byte[] packet() {
            if (this.packet == null) {
                this.packet = PacketBuf.players(this.heads, this.full, this.worldId, this.color, this.dots);
            }
            return this.packet;
        }
    }
}
