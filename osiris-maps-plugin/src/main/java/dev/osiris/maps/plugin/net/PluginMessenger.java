package dev.osiris.maps.plugin.net;

import dev.osiris.maps.plugin.OsirisMapsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

/** Solo escribe a quien anunció el canal. Un cliente sin el addon no recibe nada. */
public final class PluginMessenger {
    private final OsirisMapsPlugin plugin;

    public PluginMessenger(OsirisMapsPlugin plugin) {
        this.plugin = plugin;
        Messenger messenger = plugin.getServer().getMessenger();
        for (String channel : Channels.ALL) {
            messenger.registerOutgoingPluginChannel(plugin, channel);
        }
        messenger.registerIncomingPluginChannel(plugin, Channels.HELLO, (channel, player, message) -> {
            int revision = PacketBuf.readVarInt(message);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.isEnabled()) {
                    plugin.onHello(player, revision);
                }
            });
        });
    }

    public void send(Player player, String channel, byte[] body) {
        if (!player.getListeningPluginChannels().contains(channel)) {
            return;
        }
        sendRaw(player, channel, body);
    }

    public void sendRaw(Player player, String channel, byte[] body) {
        player.sendPluginMessage(plugin, channel, body);
    }
}
