package dev.osiris.maps.plugin;

import dev.osiris.maps.plugin.state.MapState;
import dev.osiris.maps.plugin.util.Colors;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Lee config.yml. El estado de los comandos vive aparte, en state.txt. */
public final class PluginSettings {
    private final OsirisMapsPlugin plugin;

    public PluginSettings(OsirisMapsPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        ensureAnnounceOption();
    }

    public void reload() {
        this.plugin.reloadConfig();
    }

    private void ensureAnnounceOption() {
        Path file = this.plugin.getDataFolder().toPath().resolve("config.yml");
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            String raw = Files.readString(file);
            if (raw.contains("announce-global:")) {
                return;
            }
            Files.writeString(file, "\n# true: avisa a todo el servidor al encender o apagar una opcion global o de un mundo.\nannounce-global: false\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            this.plugin.reloadConfig();
        } catch (IOException error) {
            this.plugin.getLogger().warning("No se pudo añadir announce-global a config.yml: " + error.getMessage());
        }
    }

    /** Los avisos de encendido no pasan por aqui: esos se escriben siempre. */
    public boolean logs() {
        return this.plugin.getConfig().getBoolean("logs", true);
    }

    /** Aviso a todo el servidor al encender o apagar una opcion global o de un mundo. */
    public boolean announceGlobal() {
        return this.plugin.getConfig().getBoolean("announce-global", false);
    }

    public boolean defaultDisplay(MapState.Kind kind) {
        String key = kind == MapState.Kind.MINIMAP ? "defaults.minimap" : "defaults.worldmap";
        return this.plugin.getConfig().getBoolean(key, true);
    }

    public boolean defaultRule(MapState.Rule rule) {
        String key = switch (rule) {
            case WAYPOINTS -> "defaults.waypoints";
            case DEATHPOINTS -> "defaults.deathpoints";
            case PLAYERS -> "defaults.players";
        };
        return this.plugin.getConfig().getBoolean(key, rule != MapState.Rule.PLAYERS);
    }

    public boolean removeTrackOnDeath() {
        return this.plugin.getConfig().getBoolean("tracking.remove-on-death", false);
    }

    public long trackingTicks() {
        return clamp(this.plugin.getConfig().getLong("tracking.update-ticks", 4L), 1L, 40L);
    }

    public double minMoveSquared() {
        return Math.max(0.0, this.plugin.getConfig().getDouble("tracking.min-move-squared", 0.01));
    }

    public long playersTicks() {
        return clamp(this.plugin.getConfig().getLong("players.update-ticks", 10L), 1L, 40L);
    }

    /** true = full (todo el mapa). Cualquier otro valor, incluido default, deja el alcance en la distancia de render. */
    public boolean defaultRangeFull() {
        String value = this.plugin.getConfig().getString("players.range", "default");
        return value != null && value.equalsIgnoreCase("full");
    }

    public boolean playerHeads() {
        return this.plugin.getConfig().getBoolean("players.heads", false);
    }

    public int playersColor() {
        Integer color = Colors.parse(this.plugin.getConfig().getString("players.color", "white"));
        return color == null ? 15 : color;
    }

    public long joinInterval() {
        return clamp(this.plugin.getConfig().getLong("join.interval-ticks", 10L), 1L, 100L);
    }

    public int joinAttempts() {
        return (int) clamp(this.plugin.getConfig().getLong("join.attempts", 40L), 1L, 120L);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
