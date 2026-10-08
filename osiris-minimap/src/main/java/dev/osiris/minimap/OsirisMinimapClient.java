package dev.osiris.minimap;

import dev.osiris.minimap.net.MapRules;
import dev.osiris.minimap.net.MapVisibility;
import dev.osiris.minimap.net.OsirisPayloads;
import dev.osiris.minimap.net.RemoteWaypoints;
import dev.osiris.minimap.server.MinimapSessionRefresh;
import dev.osiris.minimap.server.ServerIdentity;
import dev.osiris.minimap.server.WorldMapSessionRefresh;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class OsirisMinimapClient implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("OsirisMaps");

    @Override
    public void onInitializeClient() {
        ServerIdentity.listen(MinimapSessionRefresh::run);
        ServerIdentity.listen(WorldMapSessionRefresh::run);
        OsirisPayloads.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ServerIdentity.tick();
            RemoteWaypoints.tick();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ServerIdentity.onDisconnect();
            RemoteWaypoints.clear();
            MapVisibility.reset();
            MapRules.reset();
        }));
        LOGGER.info("OsirisMaps listo");
    }
}
