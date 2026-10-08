package dev.osiris.maps.plugin.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import dev.osiris.maps.plugin.state.PlayerMarkers;
import java.util.List;
import java.util.Map;

/**
 * Layout idéntico al codec del cliente:
 * boolean = 1 byte, texto = VarInt de bytes UTF-8, double = 8 bytes big-endian, color = VarInt.
 */
public final class PacketBuf {
    private PacketBuf() {
    }

    public static byte[] display(boolean visible) {
        return new byte[] {(byte) (visible ? 1 : 0)};
    }

    public static byte[] revision(int value) {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(5);
        writeVarInt(raw, value);
        return raw.toByteArray();
    }

    public static byte[] snapshot() {
        return new byte[] {1};
    }

    public static int readVarInt(byte[] body) {
        if (body == null || body.length == 0) {
            return 0;
        }
        int value = 0;
        int shift = 0;
        for (byte raw : body) {
            value |= (raw & 0x7F) << shift;
            if ((raw & 0x80) == 0) {
                return value;
            }
            shift += 7;
            if (shift > 28) {
                return 0;
            }
        }
        return 0;
    }

    public static byte[] point(String id, String world, double x, double y, double z, int color, String name) {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(96);
        writeUtf(raw, id);
        writeUtf(raw, world);
        writeDouble(raw, x);
        writeDouble(raw, y);
        writeDouble(raw, z);
        writeVarInt(raw, color);
        writeUtf(raw, name);
        return raw.toByteArray();
    }

    public static byte[] rules(
            boolean waypointsGlobal,
            Map<String, Boolean> waypointWorlds,
            boolean deathGlobal,
            Map<String, Boolean> deathWorlds,
            boolean playersGlobal,
            Map<String, Boolean> playerWorlds,
            boolean rangeFull,
            Map<String, Boolean> rangeWorlds
    ) {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(128);
        writeRule(raw, waypointsGlobal, waypointWorlds);
        writeRule(raw, deathGlobal, deathWorlds);
        writeRule(raw, playersGlobal, playerWorlds);
        writeRule(raw, rangeFull, rangeWorlds);
        return raw.toByteArray();
    }

    public static byte[] players(boolean heads, boolean full, String world, int color, List<PlayerMarkers.Dot> dots) {
        List<PlayerMarkers.Dot> body = full ? dots : List.of();
        ByteArrayOutputStream raw = new ByteArrayOutputStream(64 + body.size() * 52);
        raw.write(heads ? 1 : 0);
        raw.write(full ? 1 : 0);
        writeUtf(raw, world);
        writeVarInt(raw, color);
        writeVarInt(raw, body.size());
        for (PlayerMarkers.Dot dot : body) {
            writeUtf(raw, "radar:" + dot.id());
            writeDouble(raw, dot.x());
            writeDouble(raw, dot.y());
            writeDouble(raw, dot.z());
            writeFloat(raw, dot.yaw());
            writeUtf(raw, dot.name());
        }
        return raw.toByteArray();
    }

    private static void writeRule(ByteArrayOutputStream raw, boolean global, Map<String, Boolean> worlds) {
        raw.write(global ? 1 : 0);
        writeVarInt(raw, worlds == null ? 0 : worlds.size());
        if (worlds == null) {
            return;
        }
        for (Map.Entry<String, Boolean> entry : worlds.entrySet()) {
            writeUtf(raw, entry.getKey());
            raw.write(Boolean.TRUE.equals(entry.getValue()) ? 1 : 0);
        }
    }

    public static byte[] remove(String id) {
        ByteArrayOutputStream raw = new ByteArrayOutputStream(32);
        writeUtf(raw, id);
        return raw.toByteArray();
    }

    public static String trackId(java.util.UUID player) {
        return "track:" + player;
    }

    private static void writeUtf(ByteArrayOutputStream raw, String value) {
        byte[] utf8 = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        writeVarInt(raw, utf8.length);
        raw.writeBytes(utf8);
    }

    private static void writeFloat(ByteArrayOutputStream raw, float value) {
        try {
            DataOutputStream data = new DataOutputStream(raw);
            data.writeFloat(value);
            data.flush();
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void writeDouble(ByteArrayOutputStream raw, double value) {
        try {
            DataOutputStream data = new DataOutputStream(raw);
            data.writeDouble(value);
            data.flush();
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void writeVarInt(ByteArrayOutputStream raw, int value) {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            raw.write((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        raw.write(remaining);
    }
}
