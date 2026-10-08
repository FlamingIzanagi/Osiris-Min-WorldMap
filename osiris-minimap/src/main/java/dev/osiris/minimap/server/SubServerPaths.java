package dev.osiris.minimap.server;

import xaero.hud.path.XaeroPath;

/**
 * El minimapa solo crea el gestor de waypoints de terceros en contenedores de 2 nodos
 * ({@code Multiplayer_ip/dimension}). La huella va dentro del nombre del nodo raíz,
 * así la dimensión sigue siendo el segundo nodo y el mapa no crashea al entrar.
 */
public final class SubServerPaths {
    private static final String MARKER = "__";

    private SubServerPaths() {
    }

    public static XaeroPath apply(XaeroPath path) {
        if (path == null) {
            return null;
        }
        String root = path.getRoot().getLastNode();
        if (root == null || !root.startsWith("Multiplayer_")) {
            return path;
        }
        String base = baseRoot(root);
        String sub = ServerIdentity.current();
        String next = sub == null ? base : base + MARKER + sub;
        if (next.equals(root)) {
            return path;
        }
        XaeroPath rebuilt = XaeroPath.root(next);
        int count = path.getNodeCount();
        for (int i = 1; i < count; i++) {
            rebuilt = rebuilt.resolve(node(path, i));
        }
        return rebuilt;
    }

    private static String baseRoot(String root) {
        int marker = root.indexOf(MARKER);
        if (marker < 0) {
            return root;
        }
        String suffix = root.substring(marker + MARKER.length());
        if (ServerIdentity.isSubServerNode(suffix)) {
            return root.substring(0, marker);
        }
        return root;
    }

    private static String node(XaeroPath path, int index) {
        return path.getAtIndex(index).getLastNode();
    }
}
