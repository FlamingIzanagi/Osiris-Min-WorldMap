package dev.osiris.minimap.controls;

import com.mojang.blaze3d.platform.InputConstants;
import dev.osiris.minimap.mixin.KeyMappingAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.KeyMapping;

/**
 * Teclas que no deben registrarse ni aparecer en Opciones -> Controles.
 * La configuración queda en la entrada de ModMenu de Xaero's.
 */
public final class HiddenKeybinds {
    private static final Set<String> HIDDEN = Set.of(
            "gui.xaero_toggle_light_overlay",
            "gui.xaero_toggle_manual_cave_mode",
            "gui.xaero_toggle_map",
            "gui.xaero_toggle_map_waypoints",
            "gui.xaero_toggle_slime",
            "gui.xaero_toggle_tracked_players_in_world",
            "gui.xaero_toggle_tracked_players_on_map",
            "gui.xaero_toggle_tracked_players",
            "gui.xaero_display_all_sets",
            "gui.xaero_toggle_pac_chunk_claims",
            "gui.xaero_toggle_grid",
            "gui.xaero_toggle_entity_radar",
            "gui.xaero_toggle_waypoints",
            "gui.xaero_open_settings",
            "gui.xaero_minimap_settings",
            "gui.xaero_toggle_dimension",
            "gui.xaero_alternative_list_players",
            "gui.xaero_minimap_server_profiles",
            "gui.xaero_reverse_entity_radar",
            "gui.xaero_switch_waypoint_set",
            "gui.xaero_world_map_server_settings",
            "gui.xaero_map_zoom_in",
            "gui.xaero_map_zoom_out"
    );

    private HiddenKeybinds() {
    }

    public static boolean hidesTranslation(String translationKey) {
        return translationKey != null && HIDDEN.contains(translationKey);
    }

    public static void unbind(KeyMapping mapping) {
        if (mapping == null) {
            return;
        }
        mapping.setKey(InputConstants.UNKNOWN);
        KeyMappingAccessor.osiris$all().remove(mapping.getName(), mapping);
        Map<InputConstants.Key, List<KeyMapping>> byKey = KeyMappingAccessor.osiris$byKey();
        List<InputConstants.Key> empty = new ArrayList<>();
        for (Map.Entry<InputConstants.Key, List<KeyMapping>> entry : byKey.entrySet()) {
            List<KeyMapping> bindings = entry.getValue();
            if (bindings != null && bindings.remove(mapping) && bindings.isEmpty()) {
                empty.add(entry.getKey());
            }
        }
        empty.forEach(byKey::remove);
    }
}
