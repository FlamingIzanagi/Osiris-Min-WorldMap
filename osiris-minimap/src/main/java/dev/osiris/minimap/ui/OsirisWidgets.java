package dev.osiris.minimap.ui;

import dev.osiris.minimap.OsirisMinimapClient;
import dev.osiris.minimap.mixin.TooltipAccessor;
import dev.osiris.minimap.mixin.WidgetTooltipAccessor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Quita de las pantallas de Xaero el cave mode, el radar de entidades y el menú de tracked players.
 * Solo corre al abrir o al tick de una pantalla {@code xaero.*}.
 */
public final class OsirisWidgets {
    private static final String CAVE_SCREEN = "xaero.map.gui.GuiCaveModeOptions";
    private static Field caveParent;

    private OsirisWidgets() {
    }

    public static List<AbstractWidget> hiddenWidgets(Screen screen) {
        if (screen == null || !screen.getClass().getName().startsWith("xaero.")) {
            return List.of();
        }
        if (CAVE_SCREEN.equals(screen.getClass().getName())) {
            closeCaveScreen(screen);
            return List.of();
        }
        List<AbstractWidget> remove = new ArrayList<>();
        for (var child : screen.children()) {
            if (child instanceof AbstractWidget widget && hidden(widget)) {
                remove.add(widget);
            }
        }
        return remove;
    }

    private static boolean hidden(AbstractWidget widget) {
        String typeName = widget.getClass().getName();
        if (typeName.contains("Cave") || typeName.contains("MapSwitching")) {
            return true;
        }
        if (hiddenComponent(widget.getMessage())) {
            return true;
        }
        Tooltip tooltip = ((WidgetTooltipAccessor) widget).osiris$tooltip().get();
        return tooltip != null && hiddenComponent(((TooltipAccessor) (Object) tooltip).osiris$message());
    }

    private static boolean hiddenComponent(Component component) {
        if (component == null) {
            return false;
        }
        if (component.getContents() instanceof TranslatableContents contents && hiddenKey(contents.getKey())) {
            return true;
        }
        String text = component.getString();
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("cave mode")
                || lower.contains("entity radar")
                || lower.contains("tracked player")
                || lower.contains("map switching")
                || lower.contains("toggle dimension")
                || lower.contains("open settings")
                || lower.contains("chunk claim")
                || lower.contains("open parties and claims");
    }

    private static boolean hiddenKey(String key) {
        if (key == null) {
            return false;
        }
        return key.contains("cave_mode")
                || key.contains("cave_maps")
                || key.contains("entity_radar")
                || key.contains("tracked_player")
                || key.contains("open_players")
                || key.contains("close_players")
                || key.contains("map_switching")
                || key.contains("dimension_toggle")
                || key.contains("toggle_dimension")
                || key.contains("open_settings")
                || key.contains("chunk_claim")
                || key.contains("pac_claim")
                || key.contains("claims_pac")
                || key.contains("box_pac");
    }

    private static void closeCaveScreen(Screen screen) {
        Screen parent = findParent(screen);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.screen == screen) {
                minecraft.setScreen(parent);
            }
        });
    }

    private static Screen findParent(Screen screen) {
        try {
            Field field = caveParent;
            if (field == null || field.getDeclaringClass() != screen.getClass()) {
                field = null;
                for (Field candidate : screen.getClass().getDeclaredFields()) {
                    if (Screen.class.isAssignableFrom(candidate.getType())) {
                        candidate.setAccessible(true);
                        field = candidate;
                        break;
                    }
                }
                caveParent = field;
            }
            if (field == null) {
                return null;
            }
            Object value = field.get(screen);
            if (value instanceof Screen parent && parent != screen) {
                return parent;
            }
        } catch (ReflectiveOperationException error) {
            OsirisMinimapClient.LOGGER.debug("No se pudo cerrar la pantalla de cave mode", error);
        }
        return null;
    }
}
