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
import dev.osiris.maps.plugin.util.Colors;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                    help(ctx.getSource().getSender());
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
        if (worldToken != null) {
            World world = resolveWorld(worldToken);
            if (world == null) {
                err(sender, "No encuentro el mundo " + worldToken + ".");
                return 0;
            }
            worldId = MapState.worldId(world);
        }
        plugin.state().setRule(rule, worldId, enabled);
        plugin.state().broadcastRules();
        if (rule == MapState.Rule.PLAYERS && plugin.markers() != null) {
            plugin.markers().refresh();
        }
        String scope = worldId == null ? "en todo el servidor" : "en " + worldId;
        String action = enabled ? "activado" : "desactivado";
        ok(sender, ruleLabel(rule) + " " + action + " " + scope + ".");
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
        if (worldToken != null) {
            World world = resolveWorld(worldToken);
            if (world == null) {
                err(sender, "No encuentro el mundo " + worldToken + ".");
                return 0;
            }
            worldId = MapState.worldId(world);
        }
        plugin.state().setRange(worldId, full);
        plugin.state().broadcastRules();
        if (plugin.markers() != null) {
            plugin.markers().refresh();
        }
        String scope = worldId == null ? "en todo el servidor" : "en " + worldId;
        ok(sender, full
                ? "Los jugadores se ven en todo el mapa " + scope + "."
                : "Los jugadores solo se ven dentro de la distancia de render " + scope + ".");
        return 1;
    }

    private static int playerHeads(OsirisMapsPlugin plugin, CommandSender sender, boolean heads) {
        plugin.state().setPlayerHeads(heads);
        plugin.state().broadcastRules();
        if (plugin.markers() != null) {
            plugin.markers().refresh();
        }
        ok(sender, heads
                ? "Los jugadores se ven como cabezas."
                : "Los jugadores se ven como puntos.");
        return 1;
    }

    private static int reload(OsirisMapsPlugin plugin, CommandSender sender) {
        plugin.reloadSettings();
        ok(sender, "Configuracion de OsirisMaps recargada.");
        return 1;
    }

    private static String ruleLabel(MapState.Rule rule) {
        return switch (rule) {
            case WAYPOINTS -> "Crear waypoints";
            case DEATHPOINTS -> "Los waypoints de muerte";
            case PLAYERS -> "La visibilidad de jugadores";
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
        ok(ctx.getSource().getSender(), (on ? "Activado" : "Desactivado") + " el " + label(kind) + " en todo el servidor.");
        return 1;
    }

    private static int display(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx, MapState.Kind kind, boolean on) {
        Player player = online(ctx.getSource().getSender(), StringArgumentType.getString(ctx, "jugador"));
        if (player == null) {
            return 0;
        }
        plugin.state().setDisplay(player.getUniqueId(), kind, on);
        plugin.state().sendDisplay(player);
        ok(ctx.getSource().getSender(), (on ? "Activado" : "Desactivado") + " el " + label(kind) + " de " + player.getName() + ".");
        return 1;
    }

    private static int bypass(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx, MapState.Kind kind, boolean on) {
        Player self = (Player) ctx.getSource().getSender();
        plugin.state().setBypass(self.getUniqueId(), kind, on);
        plugin.state().sendDisplay(self);
        ok(self, (on ? "Bypass activado" : "Bypass desactivado") + " para tu " + label(kind) + ".");
        return 1;
    }

    private static int setWaypoint(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String id = StringArgumentType.getString(ctx, "id");
        if (!id.matches("[A-Za-z0-9_\\-]{1,32}")) {
            err(sender, "El id solo puede tener letras, números, _ y -, hasta 32 caracteres.");
            return 0;
        }
        String worldToken = StringArgumentType.getString(ctx, "mundo");
        World world = resolveWorld(worldToken);
        if (world == null) {
            err(sender, "No encuentro el mundo " + worldToken + ".");
            return 0;
        }
        Integer color = color(StringArgumentType.getString(ctx, "color"));
        if (color == null) {
            err(sender, "Color desconocido.");
            return 0;
        }
        String name = unquote(StringArgumentType.getString(ctx, "nombre"));
        if (name.isBlank() || name.length() > 32) {
            err(sender, "El nombre tiene que tener entre 1 y 32 caracteres.");
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
        ok(sender, "Waypoint " + id + " guardado en " + waypoint.world() + ".");
        return 1;
    }

    private static int removeWaypoint(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        MapState.Waypoint removed = plugin.state().removeWaypoint(id);
        if (removed == null) {
            err(ctx.getSource().getSender(), "No existe el waypoint " + id + ".");
            return 0;
        }
        plugin.state().broadcastRemove(id);
        ok(ctx.getSource().getSender(), "Waypoint " + id + " eliminado.");
        return 1;
    }

    private static int listWaypoints(OsirisMapsPlugin plugin, CommandSender sender) {
        if (plugin.state().waypoints().isEmpty()) {
            ok(sender, "No hay waypoints.");
            return 1;
        }
        for (MapState.Waypoint waypoint : plugin.state().waypoints()) {
            sender.sendMessage(Component.text(
                    waypoint.id() + " " + waypoint.world() + " "
                            + (int) Math.round(waypoint.x()) + " "
                            + (int) Math.round(waypoint.y()) + " "
                            + (int) Math.round(waypoint.z()) + " "
                            + waypoint.name(),
                    NamedTextColor.GRAY));
        }
        return 1;
    }

    private static int startTrack(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Player target = online(sender, StringArgumentType.getString(ctx, "jugador"));
        if (target == null) {
            return 0;
        }
        Integer color = color(StringArgumentType.getString(ctx, "color"));
        if (color == null) {
            err(sender, "Color desconocido.");
            return 0;
        }
        String name = unquote(StringArgumentType.getString(ctx, "nombre"));
        if (name.isBlank() || name.length() > 32) {
            err(sender, "El nombre tiene que tener entre 1 y 32 caracteres.");
            return 0;
        }
        plugin.state().putTrack(new MapState.Track(target.getUniqueId(), target.getName(), color, name));
        ok(sender, "Rastreando a " + target.getName() + " como " + name + ".");
        return 1;
    }

    private static int stopTrack(OsirisMapsPlugin plugin, CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "jugador");
        Player online = Bukkit.getPlayerExact(name);
        MapState.Track track = online != null ? plugin.state().track(online.getUniqueId()) : plugin.state().trackByName(name);
        if (track == null) {
            err(ctx.getSource().getSender(), "Ese jugador no está siendo rastreado.");
            return 0;
        }
        plugin.state().removeTrack(track.playerId());
        plugin.state().broadcastRemove(PacketBuf.trackId(track.playerId()));
        ok(ctx.getSource().getSender(), "Se dejó de rastrear a " + track.lastName() + ".");
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

    private static Player online(CommandSender sender, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            err(sender, "Ese jugador no está conectado.");
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

    private static String label(MapState.Kind kind) {
        return kind == MapState.Kind.MINIMAP ? "minimapa" : "world map";
    }

    private static void ok(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.GREEN));
    }

    private static void err(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private static void help(CommandSender sender) {
        sender.sendMessage(Component.text("/omap waypoints <on|off> [mundo]", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap deathpoints <on|off> [mundo]", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap players <on|off> [mundo]", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap playershead <on|off>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap playersrange <default|full> [mundo]", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap reload", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap display <minimap|worldmap> [jugador] <on|off>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap bypass <minimap|worldmap> <on|off>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap waypoint set <id> <mundo> <x> <y> <z> <color> <nombre>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap waypoint remove <id>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap waypoint list", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap track start <jugador> <color> <nombre>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/omap track stop <jugador>", NamedTextColor.GRAY));
    }
}
