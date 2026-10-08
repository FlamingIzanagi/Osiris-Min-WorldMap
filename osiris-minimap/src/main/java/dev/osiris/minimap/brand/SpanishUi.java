package dev.osiris.minimap.brand;

import java.util.Map;
import net.minecraft.client.Minecraft;

/** Textos de la interfaz que Xaero deja en inglés cuando el juego está en español. */
public final class SpanishUi {
    private static final String SECTION = "\u00a7";
    private static final Map<String, String> TEXT = Map.ofEntries(
            Map.entry("gui.xaero_share", "Compartir"),
            Map.entry(
                    "gui.xaero_share_msg1",
                    "¿Seguro que quieres compartir este waypoint con " + SECTION + "cTODOS" + SECTION + "f en el chat?"),
            Map.entry("gui.xaero_share_msg2", "Asegúrate de no revelar una ubicación secreta."),
            Map.entry("gui.xaero_temporary", "temporal"),
            Map.entry("gui.xaero_temporary2", "Temporal"),
            Map.entry(
                    "gui.xaero_box_visibility_type",
                    "Tipo de visibilidad en el mundo y el minimapa \n \n Local - solo se ve dentro de la distancia máxima de renderizado de waypoints \n Global - siempre visible"),
            Map.entry("gui.xaero_box_zoom_in", "%s Acercar \n (o la rueda del ratón)"),
            Map.entry("gui.xaero_box_zoom_out", "%s Alejar \n (o la rueda del ratón)"),
            Map.entry("gui.xaero_box_open_waypoints", "Abrir menú de waypoints"),
            Map.entry("gui.xaero_box_close_waypoints", "Cerrar menú de waypoints"),
            Map.entry("gui.xaero_box_open_settings", SECTION + "2%s" + SECTION + "r Abrir ajustes"),
            Map.entry("gui.xaero_box_close_settings", SECTION + "2%s" + SECTION + "r Cerrar ajustes"),
            Map.entry("gui.xaero_box_cave_mode", "Modo cueva"),
            Map.entry("gui.xaero_box_cave_mode_not_allowed", "Modo cueva (no permitido en la configuración)"),
            Map.entry(
                    "gui.xaero_dimension_toggle_button",
                    SECTION + "2%s" + SECTION + "r Cambiar dimensión (mantén Mayús para invertir)"),
            Map.entry("gui.xaero_hop_button", "Ir a las coordenadas indicadas"),
            Map.entry("gui.xaero_hop_field", "Coordenadas X y Z separadas por un espacio"),
            Map.entry("gui.xaero_hop_field_tooltip", "Escribe X y Z separadas por un espacio y pulsa ENTER"),
            Map.entry("gui.xaero_hop_error_not_numbers", "Las dos coordenadas tienen que ser números."),
            Map.entry("gui.xaero_hop_error_too_many", "Demasiados valores."),
            Map.entry("gui.xaero_hop_error_source", "Ir a coordenadas"),
            Map.entry("gui.xaero_attached_camera_button_enabled", "La cámara sigue a tu personaje"),
            Map.entry("gui.xaero_attached_camera_button_disabled", "La cámara no sigue a tu personaje"),
            Map.entry("gui.xaero_box_waypoints_minimap_required", "Los waypoints necesitan el mod del minimapa."),
            Map.entry("gui.xaero_box_waypoints_disabled", "Los waypoints están desactivados en los ajustes del mapa."),
            Map.entry("gui.xaero_box_close_players", "Cerrar menú de jugadores"),
            Map.entry("gui.xaero_box_open_players", "Abrir menú de jugadores"),
            Map.entry(
                    "gui.xaero_box_players_pac_required",
                    "El menú de jugadores del grupo necesita el mod Open Parties and Claims."),
            Map.entry("gui.xaero_box_pac_displaying_claims", "%s Mostrando claims de chunks"),
            Map.entry("gui.xaero_box_pac_not_displaying_claims", "%s Sin claims de chunks"),
            Map.entry("gui.xaero_box_claims_pac_required", "Los claims de chunks necesitan el mod Open Parties and Claims."),
            Map.entry("gui.xaero_box_rendering_waypoints", "%s Mostrando waypoints"),
            Map.entry("gui.xaero_box_not_rendering_waypoints", "%s Sin waypoints"),
            Map.entry(
                    "gui.xaero_box_rendering_waypoints_server_enforced",
                    "El servidor obliga el renderizado de waypoints del mapa."),
            Map.entry(
                    "gui.xaero_box_minimap_radar",
                    "%s El radar de entidades del minimapa está visible (necesita el minimapa)"),
            Map.entry("gui.xaero_box_no_minimap_radar", "%s El radar de entidades del minimapa no está visible"),
            Map.entry("gui.xaero_box_export", "Exportar el mapa como PNG"),
            Map.entry(
                    "gui.xaero_box_controls",
                    "Controles \n \n Haz clic y arrastra para mover el mapa. \n Gira la rueda del ratón para acercar y alejar (CTRL para precisión). \n Clic derecho en el mapa para accesos directos. \n Mantén el clic derecho y arrastra para seleccionar un área. \n Algunos botones muestran atajos extra en su descripción. \n \n Clic derecho en un elemento del mapa (por ejemplo un waypoint) para ver opciones. \n %1$sHaz clic aquí para editar los atajos."),
            Map.entry(
                    "gui.xaero_box_controls_minimap",
                    SECTION + "2%s" + SECTION + "r para crear un waypoint. \n "
                            + SECTION + "2%s" + SECTION + "r para crear un waypoint temporal rápido. \n "
                            + SECTION + "2%s" + SECTION + "r para cambiar el conjunto de waypoints. \n "
                            + SECTION + "2%s" + SECTION + "r para mostrar todos los conjuntos de waypoints. \n "
                            + SECTION + "2%s" + SECTION + "r para abrir el menú completo de waypoints. \n \n "),
            Map.entry(
                    "gui.xaero_box_controls_pac",
                    SECTION + "2%s" + SECTION + "r para abrir el menú de grupos y claims. \n \n "),
            Map.entry("gui.xaero_wm_cave_mode_start_display", "Y superior: %d"));

    private SpanishUi() {
    }

    public static String translate(String key) {
        if (key == null || !spanish()) {
            return null;
        }
        return TEXT.get(key);
    }

    private static boolean spanish() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return false;
        }
        String code = minecraft.options.languageCode;
        return code != null && code.startsWith("es");
    }
}
