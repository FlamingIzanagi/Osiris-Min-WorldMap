package dev.osiris.minimap.server;

import java.util.Objects;
import xaero.map.WorldMapSession;

/** Pide al world map que suelte el backend anterior cuando la huella cambia. */
public final class WorldMapSessionRefresh {
    private static String applied;

    private WorldMapSessionRefresh() {
    }

    public static boolean run() {
        String token = ServerIdentity.current();
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null || !session.isUsable() || session.getMapProcessor() == null) {
            return false;
        }
        if (ServerIdentity.localWorld() || Objects.equals(token, applied)) {
            return true;
        }
        session.getMapProcessor().checkForWorldUpdate();
        applied = token;
        return true;
    }
}
