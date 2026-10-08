package dev.osiris.minimap.net;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import xaero.map.gui.GuiMap;

/**
 * Canales {@code osirismaps:*}. El cuerpo es el mismo que escribe el plugin:
 * boolean en un byte, texto UTF-8 con longitud VarInt, double en big-endian y color en VarInt.
 */
public final class OsirisPayloads {
    private OsirisPayloads() {
    }

    public static void register() {
        register(DisplayMinimapPayload.TYPE, DisplayMinimapPayload.CODEC, (payload, context) ->
                context.client().execute(() -> MapVisibility.minimap(payload.visible())));
        register(DisplayWorldMapPayload.TYPE, DisplayWorldMapPayload.CODEC, (payload, context) ->
                context.client().execute(() -> {
                    MapVisibility.worldMap(payload.visible());
                    if (!payload.visible() && Minecraft.getInstance().screen instanceof GuiMap) {
                        Minecraft.getInstance().setScreen(null);
                    }
                }));
        register(AddWaypointPayload.TYPE, AddWaypointPayload.CODEC, (payload, context) ->
                context.client().execute(() -> RemoteWaypoints.add(
                        payload.waypointId(), payload.world(), payload.x(), payload.y(), payload.z(), payload.color(), payload.name(), false)));
        register(RemoveWaypointPayload.TYPE, RemoveWaypointPayload.CODEC, (payload, context) ->
                context.client().execute(() -> RemoteWaypoints.remove(payload.waypointId())));
        register(UpdateTrackingPayload.TYPE, UpdateTrackingPayload.CODEC, (payload, context) ->
                context.client().execute(() -> RemoteWaypoints.track(
                        payload.waypointId(), payload.world(), payload.x(), payload.y(), payload.z(), payload.color(), payload.name())));
        register(MapRulesPayload.TYPE, MapRulesPayload.CODEC, (payload, context) ->
                context.client().execute(() -> MapRules.replace(
                        payload.waypointsGlobal(), payload.waypointWorlds(),
                        payload.deathGlobal(), payload.deathWorlds(),
                        payload.playersGlobal(), payload.playerWorlds(),
                        payload.rangeFull(), payload.rangeWorlds())));
        register(PlayersPayload.TYPE, PlayersPayload.CODEC, (payload, context) ->
                context.client().execute(() -> RemoteWaypoints.syncRadar(payload.world(), payload.heads(), payload.full(), payload.players())));
        register(SnapshotPayload.TYPE, SnapshotPayload.CODEC, (payload, context) ->
                context.client().execute(RemoteWaypoints::dropStatic));
        register(RevisionPayload.TYPE, RevisionPayload.CODEC, (payload, context) ->
                context.client().execute(() -> RuleProfile.remember(payload.revision())));
        PayloadTypeRegistry.playC2S().register(HelloPayload.TYPE, HelloPayload.CODEC);
    }

    private static <T extends CustomPacketPayload> void register(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            ClientPlayNetworking.PlayPayloadHandler<T> handler
    ) {
        PayloadTypeRegistry.playS2C().register(type, codec);
        ClientPlayNetworking.registerGlobalReceiver(type, handler);
    }

    private static Identifier channel(String path) {
        return Identifier.fromNamespaceAndPath("osirismaps", path);
    }

    public record DisplayMinimapPayload(boolean visible) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DisplayMinimapPayload> TYPE = new CustomPacketPayload.Type<>(channel("display_minimap"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DisplayMinimapPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.visible()),
                buf -> new DisplayMinimapPayload(buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DisplayWorldMapPayload(boolean visible) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DisplayWorldMapPayload> TYPE = new CustomPacketPayload.Type<>(channel("display_worldmap"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DisplayWorldMapPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.visible()),
                buf -> new DisplayWorldMapPayload(buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AddWaypointPayload(String waypointId, String world, double x, double y, double z, int color, String name) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AddWaypointPayload> TYPE = new CustomPacketPayload.Type<>(channel("add_waypoint"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AddWaypointPayload> CODEC = StreamCodec.of(
                (buf, payload) -> writePoint(buf, payload.waypointId(), payload.world(), payload.x(), payload.y(), payload.z(), payload.color(), payload.name()),
                buf -> new AddWaypointPayload(buf.readUtf(), buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RemoveWaypointPayload(String waypointId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RemoveWaypointPayload> TYPE = new CustomPacketPayload.Type<>(channel("remove_waypoint"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveWaypointPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeUtf(payload.waypointId()),
                buf -> new RemoveWaypointPayload(buf.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UpdateTrackingPayload(String waypointId, String world, double x, double y, double z, int color, String name) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<UpdateTrackingPayload> TYPE = new CustomPacketPayload.Type<>(channel("update_tracking"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTrackingPayload> CODEC = StreamCodec.of(
                (buf, payload) -> writePoint(buf, payload.waypointId(), payload.world(), payload.x(), payload.y(), payload.z(), payload.color(), payload.name()),
                buf -> new UpdateTrackingPayload(buf.readUtf(), buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MapRulesPayload(
            boolean waypointsGlobal,
            Map<String, Boolean> waypointWorlds,
            boolean deathGlobal,
            Map<String, Boolean> deathWorlds,
            boolean playersGlobal,
            Map<String, Boolean> playerWorlds,
            boolean rangeFull,
            Map<String, Boolean> rangeWorlds
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MapRulesPayload> TYPE = new CustomPacketPayload.Type<>(channel("map_rules"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MapRulesPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    writeRule(buf, payload.waypointsGlobal(), payload.waypointWorlds());
                    writeRule(buf, payload.deathGlobal(), payload.deathWorlds());
                    writeRule(buf, payload.playersGlobal(), payload.playerWorlds());
                    writeRule(buf, payload.rangeFull(), payload.rangeWorlds());
                },
                buf -> new MapRulesPayload(
                        buf.readBoolean(), readRule(buf),
                        buf.readBoolean(), readRule(buf),
                        buf.readBoolean(), readRule(buf),
                        buf.readBoolean(), readRule(buf))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record HelloPayload(int revision) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HelloPayload> TYPE = new CustomPacketPayload.Type<>(channel("hello"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeVarInt(payload.revision()),
                buf -> new HelloPayload(buf.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SnapshotPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SnapshotPayload> TYPE = new CustomPacketPayload.Type<>(channel("snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeByte(1),
                buf -> {
                    buf.readByte();
                    return new SnapshotPayload();
                }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RevisionPayload(int revision) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RevisionPayload> TYPE = new CustomPacketPayload.Type<>(channel("revision"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RevisionPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeVarInt(payload.revision()),
                buf -> new RevisionPayload(buf.readVarInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlayersPayload(boolean heads, boolean full, String world, int color, List<RemoteWaypoints.RadarDot> players) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlayersPayload> TYPE = new CustomPacketPayload.Type<>(channel("players"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayersPayload> CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.heads());
                    buf.writeBoolean(payload.full());
                    buf.writeUtf(payload.world());
                    buf.writeVarInt(payload.color());
                    buf.writeVarInt(payload.players().size());
                    for (RemoteWaypoints.RadarDot dot : payload.players()) {
                        buf.writeUtf(dot.id());
                        buf.writeDouble(dot.x());
                        buf.writeDouble(dot.y());
                        buf.writeDouble(dot.z());
                        buf.writeFloat(dot.yaw());
                        buf.writeUtf(dot.name());
                    }
                },
                buf -> {
                    boolean heads = buf.readBoolean();
                    boolean full = buf.readBoolean();
                    String world = buf.readUtf();
                    int color = buf.readVarInt();
                    int count = buf.readVarInt();
                    List<RemoteWaypoints.RadarDot> players = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        players.add(new RemoteWaypoints.RadarDot(
                                buf.readUtf(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readUtf()));
                    }
                    return new PlayersPayload(heads, full, world, color, players);
                }
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void writeRule(RegistryFriendlyByteBuf buf, boolean global, Map<String, Boolean> worlds) {
        buf.writeBoolean(global);
        buf.writeVarInt(worlds.size());
        for (Map.Entry<String, Boolean> entry : worlds.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeBoolean(entry.getValue());
        }
    }

    private static Map<String, Boolean> readRule(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<String, Boolean> worlds = new HashMap<>();
        for (int i = 0; i < count; i++) {
            worlds.put(buf.readUtf(), buf.readBoolean());
        }
        return worlds;
    }

    private static void writePoint(RegistryFriendlyByteBuf buf, String id, String world, double x, double y, double z, int color, String name) {
        buf.writeUtf(id);
        buf.writeUtf(world);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeVarInt(color);
        buf.writeUtf(name);
    }
}
