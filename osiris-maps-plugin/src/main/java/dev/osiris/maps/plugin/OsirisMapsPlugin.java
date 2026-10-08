package dev.osiris.maps.plugin;

import dev.osiris.maps.plugin.command.OmapCommand;
import dev.osiris.maps.plugin.net.PacketBuf;
import dev.osiris.maps.plugin.net.PluginMessenger;
import dev.osiris.maps.plugin.state.MapState;
import dev.osiris.maps.plugin.state.PlayerMarkers;
import dev.osiris.maps.plugin.state.StateFile;
import dev.osiris.maps.plugin.text.PluginMessages;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class OsirisMapsPlugin extends JavaPlugin implements Listener {
    private PluginSettings settings;
    private PluginMessages messages;
    private MapState state;
    private PluginMessenger messenger;
    private PlayerMarkers markers;
    private final Map<UUID, Integer> greeted = new HashMap<>();

    public OsirisMapsPlugin() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(OmapCommand.builder(this).build(), "Administra los mapas de Osiris"));
    }

    @Override
    public void onEnable() {
        this.settings = new PluginSettings(this);
        this.messages = new PluginMessages(this);
        this.messages.load();
        this.state = new MapState(this);
        this.messenger = new PluginMessenger(this);
        this.state.load(StateFile.read(this));
        this.markers = new PlayerMarkers(this);
        this.markers.start();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Enabled");
        getLogger().info("Developed by FlamingIzanagi");
    }

    @Override
    public void onDisable() {
        if (this.markers != null) {
            this.markers.stop();
        }
        if (this.state != null) {
            this.state.shutdown();
        }
    }

    public void reloadSettings() {
        this.settings.reload();
        this.messages.load();
        this.state.restartTrackTask();
        this.markers.start();
    }

    /**
     * El addon avisa la revisión que ya guardó. Si coincide, no se reenvían las reglas.
     * Sin este paquete el jugador no tiene el addon y no recibe nada.
     */
    public void onHello(Player player, int revision) {
        if (player == null || !player.isOnline() || this.state == null) {
            return;
        }
        Integer seen = this.greeted.put(player.getUniqueId(), revision);
        if (seen != null && seen == revision) {
            return;
        }
        long current = this.state.revision();
        if (revision == current) {
            this.state.pushTracks(player);
            if (this.settings.logs()) {
                getLogger().info(player.getName() + " ya tiene la revision " + current + ". No se reenviaron las configuraciones.");
            }
            return;
        }
        this.state.pushPlayer(player);
        if (this.settings.logs()) {
            getLogger().info("Enviadas las configuraciones a " + player.getName() + " (tenia la revision " + revision + ", servidor " + current + ").");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.greeted.remove(id);
        if (this.markers != null) {
            this.markers.forget(id);
        }
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        if (this.markers != null) {
            this.markers.send(event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!this.settings.removeTrackOnDeath()) {
            return;
        }
        Player player = event.getEntity();
        MapState.Track track = this.state.track(player.getUniqueId());
        if (track == null) {
            return;
        }
        this.state.removeTrack(player.getUniqueId());
        this.state.broadcastRemove(PacketBuf.trackId(player.getUniqueId()));
    }

    public PluginSettings settings() {
        return this.settings;
    }

    public PluginMessages messages() {
        return this.messages;
    }

    public MapState state() {
        return this.state;
    }

    public PluginMessenger messenger() {
        return this.messenger;
    }

    public PlayerMarkers markers() {
        return this.markers;
    }

}
