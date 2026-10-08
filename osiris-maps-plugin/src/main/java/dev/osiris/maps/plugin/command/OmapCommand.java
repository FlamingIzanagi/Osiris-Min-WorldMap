package dev.osiris.maps.plugin.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.osiris.maps.plugin.OsirisMapsPlugin;
import dev.osiris.maps.plugin.net.PacketBuf;
import dev.osiris.maps.plugin.state.MapState;
import dev.osiris.maps.plugin.text.PluginMessages;
import dev.osiris.maps.plugin.util.Colors;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /omap. El permiso osirismaps.admin cubre todo menos el bypass, que además exige OP. */
public final class OmapCommand {
    private OmapCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> builder(OsirisMapsPlugin plugin) {
        return literal("omap")
                .requires(source -> source.getSender().hasPermission("osirismaps.admin"))
                .executes(ctx -> {
                    plugin.messages().help(ctx.getSource().getSender());
                    return 1;
                })
                .then(literal("display")
                        .then(displayKind(plugin, "minimap", MapState.Kind.MINIMAP))
                        .then(displayKind(plugin, "worldmap", MapState.Kind.WORLDMAP)))
                .then(literal("bypass")
                        .requires(source -> source.getSender().isOp() && source.getSender() instanceof Player)
                        .then(bypassKind(plugin, "minimap", MapState.Kind.MINIMAP))
                        .then(bypassKind(plugin, "worldmap", MapState.Kind.WORLDMAP)))
                .then(ruleCommand(plugin, "waypoints", MapState.Rule.WAYPOINTS))
                .then(ruleCommand(plugin, "deathpoints", MapState.Rule.DEATHPOINTS))
                .then(ruleCommand(plugin, "players", MapState.Rule.PLAYERS))
                .then(literal("playershead")
                        .then(literal("on").executes(ctx -> playerHeads(plugin, ctx.getSource().getSender(), true)))
                        .then(literal("off").executes(ctx -> playerHeads(plugin, ctx.getSource().getSender(), false))))
                .then(literal("playersrange")
                        .then(rangeChoice(plugin, "default", false))
                        .then(rangeChoice(plugin, "full", true)))
                .then(literal("reload").executes(ctx -> reload(plugin, ctx.getSource().getSender())))
                .then(literal("waypoint")
                        .then(literal("set")
                                .then(word("id").then(text("mundo").suggests((ctx, builder) -> {
                                    suggestWorlds(builder);
                                    return builder.buildFuture();
                                }).then(doubleArg("x").then(doubleArg("y").then(doubleArg("z").then(word("color").suggests((ctx, builder) -> {
                                    suggestColors(builder);
                                    return builder.buildFuture();
                                }).then(greedy("nombre").executes(ctx -> setWaypoint(plugin, ctx))))))))))
                        .then(literal("remove")
                                .then(word("id").suggests((ctx, builder) -> {
                                    String prefix = builder.getRemainingLowerCase();
                                    for (MapState.Waypoint waypoint : plugin.state().waypoints()) {
                                        suggest(builder, waypoint.id(), prefix);
                                    }
                                    return builder.buildFuture();
                                }).executes(ctx -> removeWaypoint(plugin, ctx))))
                        .then(literal("list").executes(ctx -> listWaypoints(plugin, ctx.getSource().getSender()))))
                .then(literal("track")
                        .then(literal("start")
                                .then(playerArg().then(word("color").suggests((ctx, builder) -> {
                                    suggestColors(builder);
                                    return builder.buildFuture();
                                }).then(greedy("nombre").executes(ctx -> startTrack(plugin, ctx))))))
                        .then(literal("stop")
                                .then(word("jugador").suggests((ctx, builder) -> {
                                    suggestPlayers(builder);
                                    return builder.buildFuture();
                                }).executes(ctx -> stopTrack(plugin, ctx)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ruleCommand(OsirisMapsPlugin plugin, String name, MapState.Rule rule) {
        return literal(name)
                .then(ruleState(plugin, "on", rule, true))
                .then(ruleState(plugin, "off", rule, false));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> ruleState(OsirisMapsPlugin plugin, String name, MapState.Rule rule, boolean enabled) {
        return literal(name)
                .executes(ctx -> applyRule(plugin, ctx, rule, enabled, null))
                .then(text("mundo").suggests((ctx, builder) -> {
                    suggestWorlds(builder);
                    return builder.buildFuture();
                }).executes(ctx -> applyRule(plugin, ctx, rule, enabled, StringArgumentType.getString(ctx, "mundo"))));
    }

    private static int applyRule(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx, MapState.Rule rule, boolean enabled, String worldToken) {
        CommandSender sender = ctx.getSource().getSender();
        String worldId = null;
        String worldName = null;
        if (worldToken != null) {
            World world = resolveWorld(worldToken);
            if (world == null) {
                plugin.messages().send(sender, "error-world", "world", worldToken);
                return 0;
            }
            worldId = MapState.worldId(world);
            worldName = world.getName();
        }
        plugin.state().setRule(rule, worldId, enabled);
        plugin.state().broadcastRules();
        if (rule == MapState.Rule.PLAYERS && plugin.markers() != null) {
            plugin.markers().refresh();
        }
        plugin.state().publish();
        String key = (worldId == null ? "rule-global-" : "rule-world-") + (enabled ? "on" : "off");
        plugin.messages().send(sender, key, true, "label", plugin.messages().text(ruleLabel(rule)), "world", worldName == null ? "" : worldName);
        announce(plugin, worldName, enabled, "announce-change-" + rule.name().toLowerCase(Locale.ROOT), sender);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rangeChoice(OsirisMapsPlugin plugin, String name, boolean full) {
        return literal(name)
                .executes(ctx -> applyRange(plugin, ctx, full, null))
                .then(text("mundo").suggests((ctx, builder) -> {
                    suggestWorlds(builder);
                    return builder.buildFuture();
                }).executes(ctx -> applyRange(plugin, ctx, full, StringArgumentType.getString(ctx, "mundo"))));
    }

    private static int applyRange(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx, boolean full, String worldToken) {
        CommandSender sender = ctx.getSource().getSender();
        String worldId = null;
        String worldName = null;
        if (worldToken != null) {
            World world = resolveWorld(worldToken);
            if (world == null) {
                plugin.messages().send(sender, "error-world", "world", worldToken);
                return 0;
            }
            worldId = MapState.worldId(world);
            worldName = world.getName();
        }
        plugin.state().setRange(worldId, full);
        plugin.state().broadcastRules();
        if (plugin.markers() != null) {
            plugin.markers().refresh();
        }
        plugin.state().publish();
        String scope = worldId == null
                ? plugin.messages().text("scope-global")
                : plugin.messages().text("scope-world").replace("{world}", worldName);
        plugin.messages().send(sender, full ? "playersrange-full" : "playersrange-default", true, "scope", scope);
        announce(plugin, worldName, full, "announce-change-range", sender);
        return 1;
    }

    private static int playerHeads(OsirisMapsPlugin plugin, CommandSender sender, boolean heads) {
        plugin.state().setPlayerHeads(heads);
        plugin.state().broadcastRules();
        if (plugin.markers() != null) {
            plugin.markers().refresh();
        }
        plugin.state().publish();
        plugin.messages().send(sender, heads ? "playershead-on" : "playershead-off", true);
        announce(plugin, null, heads, "announce-change-playershead", sender);
        return 1;
    }

    private static int reload(OsirisMapsPlugin plugin, CommandSender sender) {
        plugin.reloadSettings();
        plugin.messages().send(sender, "reload");
        return 1;
    }

    private static String ruleLabel(MapState.Rule rule) {
        return switch (rule) {
            case WAYPOINTS -> "label-waypoints";
            case DEATHPOINTS -> "label-deathpoints";
            case PLAYERS -> "label-players";
        };
    }

    private static LiteralArgumentBuilder<CommandSourceStack> displayKind(OsirisMapsPlugin plugin, String name, MapState.Kind kind) {
        return literal(name)
                .then(literal("on").executes(ctx -> displayGlobal(plugin, ctx, kind, true)))
                .then(literal("off").executes(ctx -> displayGlobal(plugin, ctx, kind, false)))
                .then(playerArg()
                        .then(literal("on").executes(ctx -> display(plugin, ctx, kind, true)))
                        .then(literal("off").executes(ctx -> display(plugin, ctx, kind, false))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> bypassKind(OsirisMapsPlugin plugin, String name, MapState.Kind kind) {
        return literal(name)
                .then(literal("on").executes(ctx -> bypass(plugin, ctx, kind, true)))
                .then(literal("off").executes(ctx -> bypass(plugin, ctx, kind, false)));
    }

    private static int displayGlobal(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx, MapState.Kind kind, boolean on) {
        plugin.state().setGlobalDisplay(kind, on);
        plugin.state().broadcastDisplay();
        plugin.state().publish();
        plugin.messages().send(ctx.getSource().getSender(), on ? "display-global-on" : "display-global-off", true, "kind", kindName(plugin.messages(), kind));
        announce(plugin, null, on, kind == MapState.Kind.MINIMAP ? "announce-change-minimap" : "announce-change-worldmap", ctx.getSource().getSender());
        return 1;
    }

    private static int display(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx, MapState.Kind kind, boolean on) {
        Player player = online(plugin, ctx.getSource().getSender(), StringArgumentType.getString(ctx, "jugador"));
        if (player == null) {
            return 0;
        }
        plugin.state().setDisplay(player.getUniqueId(), kind, on);
        plugin.state().sendDisplay(player);
        plugin.state().publish();
        plugin.messages().send(ctx.getSource().getSender(), on ? "display-player-on" : "display-player-off", true,
                "kind", kindName(plugin.messages(), kind), "player", player.getName());
        return 1;
    }

    private static int bypass(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx, MapState.Kind kind, boolean on) {
        Player self = (Player) ctx.getSource().getSender();
        plugin.state().setBypass(self.getUniqueId(), kind, on);
        plugin.state().sendDisplay(self);
        plugin.state().publish();
        plugin.messages().send(self, on ? "bypass-on" : "bypass-off", true, "kind", kindName(plugin.messages(), kind));
        return 1;
    }

    private static int setWaypoint(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String id = StringArgumentType.getString(ctx, "id");
        if (!id.matches("[A-Za-z0-9_\\-]{1,32}")) {
            plugin.messages().send(sender, "error-id");
            return 0;
        }
        String worldToken = StringArgumentType.getString(ctx, "mundo");
        World world = resolveWorld(worldToken);
        if (world == null) {
            plugin.messages().send(sender, "error-world", "world", worldToken);
            return 0;
        }
        Integer color = color(StringArgumentType.getString(ctx, "color"));
        if (color == null) {
            plugin.messages().send(sender, "error-color");
            return 0;
        }
        String name = unquote(StringArgumentType.getString(ctx, "nombre"));
        if (name.isBlank() || name.length() > 32) {
            plugin.messages().send(sender, "error-name");
            return 0;
        }
        MapState.Waypoint waypoint = new MapState.Waypoint(
                id,
                MapState.worldId(world),
                DoubleArgumentType.getDouble(ctx, "x"),
                DoubleArgumentType.getDouble(ctx, "y"),
                DoubleArgumentType.getDouble(ctx, "z"),
                color,
                name
        );
        plugin.state().putWaypoint(waypoint);
        plugin.state().broadcastWaypoint(waypoint);
        plugin.state().publish();
        plugin.messages().send(sender, "waypoint-set", "id", id, "world", waypoint.world());
        return 1;
    }

    private static int removeWaypoint(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        MapState.Waypoint removed = plugin.state().removeWaypoint(id);
        if (removed == null) {
            plugin.messages().send(ctx.getSource().getSender(), "error-waypoint", "id", id);
            return 0;
        }
        plugin.state().broadcastRemove(id);
        plugin.state().publish();
        plugin.messages().send(ctx.getSource().getSender(), "waypoint-remove", "id", id);
        return 1;
    }

    private static int listWaypoints(OsirisMapsPlugin plugin, CommandSender sender) {
        if (plugin.state().waypoints().isEmpty()) {
            plugin.messages().send(sender, "waypoint-empty");
            return 1;
        }
        for (MapState.Waypoint waypoint : plugin.state().waypoints()) {
            plugin.messages().send(sender, "waypoint-line",
                    "id", waypoint.id(),
                    "world", waypoint.world(),
                    "x", Integer.toString((int) Math.round(waypoint.x())),
                    "y", Integer.toString((int) Math.round(waypoint.y())),
                    "z", Integer.toString((int) Math.round(waypoint.z())),
                    "name", waypoint.name());
        }
        return 1;
    }

    private static int startTrack(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Player target = online(plugin, sender, StringArgumentType.getString(ctx, "jugador"));
        if (target == null) {
            return 0;
        }
        Integer color = color(StringArgumentType.getString(ctx, "color"));
        if (color == null) {
            plugin.messages().send(sender, "error-color");
            return 0;
        }
        String name = unquote(StringArgumentType.getString(ctx, "nombre"));
        if (name.isBlank() || name.length() > 32) {
            plugin.messages().send(sender, "error-name");
            return 0;
        }
        plugin.state().putTrack(new MapState.Track(target.getUniqueId(), target.getName(), color, name));
        plugin.messages().send(sender, "track-start", "player", target.getName(), "name", name);
        return 1;
    }

    private static int stopTrack(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "jugador");
        Player online = Bukkit.getPlayerExact(name);
        MapState.Track track = online != null ? plugin.state().track(online.getUniqueId()) : plugin.state().trackByName(name);
        if (track == null) {
            plugin.messages().send(ctx.getSource().getSender(), "error-track");
            return 0;
        }
        plugin.state().removeTrack(track.playerId());
        plugin.state().broadcastRemove(PacketBuf.trackId(track.playerId()));
        plugin.messages().send(ctx.getSource().getSender(), "track-stop", "player", track.lastName());
        return 1;
    }

    private static World resolveWorld(String token) {
        World direct = Bukkit.getWorld(token);
        if (direct != null) {
            return direct;
        }
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().equalsIgnoreCase(token) || world.getKey().asString().equalsIgnoreCase(token)) {
                return world;
            }
        }
        return null;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> word(String name) {
        return RequiredArgumentBuilder.argument(name, StringArgumentType.word());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> text(String name) {
        return RequiredArgumentBuilder.argument(name, StringArgumentType.string());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> greedy(String name) {
        return RequiredArgumentBuilder.argument(name, StringArgumentType.greedyString());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Double> doubleArg(String name) {
        return RequiredArgumentBuilder.argument(name, DoubleArgumentType.doubleArg());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> playerArg() {
        return word("jugador").suggests((ctx, builder) -> {
            suggestPlayers(builder);
            return builder.buildFuture();
        });
    }

    private static void suggestPlayers(SuggestionsBuilder builder) {
        String prefix = builder.getRemainingLowerCase();
        for (Player player : Bukkit.getOnlinePlayers()) {
            suggest(builder, player.getName(), prefix);
        }
    }

    private static void suggestWorlds(SuggestionsBuilder builder) {
        String prefix = builder.getRemainingLowerCase();
        for (World world : Bukkit.getWorlds()) {
            suggest(builder, world.getName(), prefix);
            suggest(builder, world.getKey().asString(), prefix);
        }
    }

    private static void suggestColors(SuggestionsBuilder builder) {
        String prefix = builder.getRemainingLowerCase();
        for (String color : Colors.names()) {
            suggest(builder, color, prefix);
        }
    }

    private static void suggest(SuggestionsBuilder builder, String value, String prefix) {
        if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            builder.suggest(value);
        }
    }

    private static Player online(OsirisMapsPlugin plugin, CommandSender sender, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            plugin.messages().send(sender, "error-offline");
        }
        return player;
    }

    private static Integer color(String token) {
        return Colors.parse(token);
    }

    private static String unquote(String raw) {
        String name = raw.trim();
        if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
            return name.substring(1, name.length() - 1).trim();
        }
        return name;
    }

    private static void announce(OsirisMapsPlugin plugin, String worldName, boolean enabled, String changeKey, CommandSender sender) {
        String key = (worldName == null ? "announce-global-" : "announce-world-") + (enabled ? "on" : "off");
        plugin.messages().announce(key,
                "change", plugin.messages().text(changeKey),
                "world", worldName == null ? "" : worldName,
                "player", sender.getName());
    }

    private static String kindName(PluginMessages messages, MapState.Kind kind) {
        return messages.text(kind == MapState.Kind.MINIMAP ? "kind-minimap" : "kind-worldmap");
    }
}
