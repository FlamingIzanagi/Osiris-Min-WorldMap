package dev.osiris.minimap.server;

import java.util.Objects;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.MinimapProcessor;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.state.MinimapWorldStateUpdater;

/**
 * Fuerza a Xaero a releer la ruta cuando cambia el backend.
 * Se ejecuta en el hilo del cliente y no retiene mundos anteriores.
 */
public final class MinimapSessionRefresh {
    private static String applied;

    private MinimapSessionRefresh() {
    }

    @SuppressWarnings("deprecation")
    public static boolean run() {
        String token = ServerIdentity.current();
        XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
        if (session == null) {
            return false;
        }
        MinimapProcessor processor = session.getMinimapProcessor();
        if (processor == null) {
            return false;
        }
        MinimapSession module = processor.getSession();
        if (module == null) {
            return false;
        }
        if (ServerIdentity.localWorld() || Objects.equals(token, applied)) {
            return true;
        }
        MinimapWorldStateUpdater updater = module.getWorldStateUpdater();
        if (updater != null) {
            updater.update();
        }
        applied = token;
        return true;
    }
}
