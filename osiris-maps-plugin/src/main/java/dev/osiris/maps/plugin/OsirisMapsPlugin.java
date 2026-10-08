package dev.osiris.maps.plugin;

import dev.osiris.maps.plugin.command.OmapCommand;
import dev.osiris.maps.plugin.net.Channels;
import dev.osiris.maps.plugin.net.PacketBuf;
import dev.osiris.maps.plugin.net.PluginMessenger;
import dev.osiris.maps.plugin.state.MapState;
import dev.osiris.maps.plugin.state.PlayerMarkers;
import dev.osiris.maps.plugin.state.StateFile;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class OsirisMapsPlugin extends JavaPlugin implements Listener {
    private PluginSettings settings;
    private MapState state;
    private PluginMessenger messenger;
    private PlayerMarkers markers;
    private final Set<UUID> syncing = new HashSet<>();

    public OsirisMapsPlugin() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(OmapCommand.builder(this).build(), "Administra los mapas de Osiris"));
    }

    @Override
    public void onEnable() {
        this.settings = new PluginSettings(this);
        this.state = new MapState(this);
        this.messenger = new PluginMessenger(this);
        this.state.load(StateFile.read(this));
        this.markers = new PlayerMarkers(this);
        this.markers.start();
        Bukkit.getPluginManager().registerEvents(this, this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            schedulePush(player);
        }
        getLogger().info("OsirisMaps 1.0.0 listo");
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
        this.state.restartTrackTask();
        this.markers.start();
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.state.pushPlayer(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        schedulePush(event.getPlayer());
    }

    @EventHandler
    public void onChannel(PlayerRegisterChannelEvent event) {
        if (event.getChannel().startsWith("osirismaps:")) {
            schedulePush(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.syncing.remove(id);
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

    public MapState state() {
        return this.state;
    }

    public PluginMessenger messenger() {
        return this.messenger;
    }

    public PlayerMarkers markers() {
        return this.markers;
    }

    private void schedulePush(Player player) {
        UUID id = player.getUniqueId();
        if (!this.syncing.add(id)) {
            return;
        }
        long interval = this.settings.joinInterval();
        int attempts = this.settings.joinAttempts();
        new BukkitRunnable() {
            private int tries;

            @Override
            public void run() {
                if (!player.isOnline() || ++this.tries > attempts) {
                    syncing.remove(id);
                    cancel();
                    return;
                }
                if (!ready(player)) {
                    return;
                }
                state.pushPlayer(player);
                syncing.remove(id);
                cancel();
            }
        }.runTaskTimer(this, 1L, interval);
    }

    private static boolean ready(Player player) {
        var listening = player.getListeningPluginChannels();
        return listening.contains(Channels.MAP_RULES)
                && listening.contains(Channels.DISPLAY_MINIMAP)
                && listening.contains(Channels.DISPLAY_WORLDMAP)
                && listening.contains(Channels.PLAYERS)
                && listening.contains(Channels.ADD_WAYPOINT);
    }
}
