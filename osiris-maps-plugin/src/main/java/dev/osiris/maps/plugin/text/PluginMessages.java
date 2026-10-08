package dev.osiris.maps.plugin.text;

import dev.osiris.maps.plugin.OsirisMapsPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/** messages.yml. El prefijo se puede apagar o cambiar sin tocar el codigo. */
public final class PluginMessages {
    private final OsirisMapsPlugin plugin;
    private FileConfiguration config = new YamlConfiguration();

    public PluginMessages(OsirisMapsPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(this.plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            this.plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        appendAnnounceKeys(file);
    }

    /** No reescribe el archivo: solo añade las claves del anuncio si faltan. */
    private void appendAnnounceKeys(File file) {
        if (this.config.contains("announce-global-on")) {
            return;
        }
        String extra = "\n# Anuncio a todos. Solo sale si announce-global esta en true en config.yml.\n"
                + "announce-global-on: \"&eSe ha activado {change} en el servidor.\"\n"
                + "announce-global-off: \"&eSe ha desactivado {change} en el servidor.\"\n"
                + "announce-world-on: \"&eSe ha activado {change} en el mundo {world}.\"\n"
                + "announce-world-off: \"&eSe ha desactivado {change} en el mundo {world}.\"\n"
                + "announce-change-minimap: \"el minimapa\"\n"
                + "announce-change-worldmap: \"el world map\"\n"
                + "announce-change-waypoints: \"la creacion de waypoints\"\n"
                + "announce-change-deathpoints: \"los waypoints de muerte\"\n"
                + "announce-change-players: \"la visibilidad de jugadores\"\n"
                + "announce-change-playershead: \"el modo de cabezas\"\n"
                + "announce-change-range: \"el alcance completo de jugadores\"\n";
        try {
            Files.writeString(file.toPath(), extra, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            this.config = YamlConfiguration.loadConfiguration(file);
        } catch (IOException error) {
            this.plugin.getLogger().warning("No se pudieron añadir los mensajes de anuncio: " + error.getMessage());
        }
    }

    public void announce(String key, String... tokens) {
        if (!this.plugin.settings().announceGlobal()) {
            return;
        }
        Bukkit.broadcast(render(text(key), tokens));
    }

    public String text(String key) {
        return this.config.getString(key, key);
    }

    public void send(CommandSender sender, String key, String... tokens) {
        send(sender, key, false, tokens);
    }

    public void send(CommandSender sender, String key, boolean console, String... tokens) {
        Component message = render(text(key), tokens);
        sender.sendMessage(message);
        if (console && !(sender instanceof ConsoleCommandSender)) {
            this.plugin.getComponentLogger().info(message);
        }
    }

    public void help(CommandSender sender) {
        List<String> lines = this.config.getStringList("help");
        if (lines.isEmpty()) {
            sender.sendMessage(render(text("help"), new String[0]));
            return;
        }
        for (String line : lines) {
            sender.sendMessage(render(line, new String[0]));
        }
    }

    private Component render(String template, String... tokens) {
        Component body = MessageText.parse(MessageText.fill(template, tokens));
        if (!this.config.getBoolean("prefix-enabled", true)) {
            return body;
        }
        String prefix = this.config.getString("prefix", "");
        if (prefix == null || prefix.isEmpty()) {
            return body;
        }
        return MessageText.parse(prefix).append(body);
    }
}
