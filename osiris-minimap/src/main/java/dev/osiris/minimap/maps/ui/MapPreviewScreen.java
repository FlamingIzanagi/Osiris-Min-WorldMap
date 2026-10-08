package dev.osiris.minimap.maps.ui;

import dev.osiris.minimap.maps.MapPreview;
import dev.osiris.minimap.maps.SavedMap;
import dev.osiris.minimap.maps.SavedMapStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;

/**
 * Abre el world map en vivo cuando el mapa es el de esta sesión.
 * Si el mapa es de otro mundo, muestra esta pantalla: el renderizador solo tiene cargado el mundo actual.
 */
public class MapPreviewScreen extends Screen {
    private final Screen management;
    private final Screen waypoints;
    private final SavedMap map;

    public MapPreviewScreen(Screen management, Screen waypoints, SavedMap map) {
        super(Component.literal("Previsualización"));
        this.management = management;
        this.waypoints = waypoints;
        this.map = map;
    }

    public static void open(Screen management, Screen waypoints, SavedMap map) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (SavedMapStore.isLoaded(map) && minecraft.player != null) {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            if (session != null && session.getMapProcessor() != null) {
                MapPreview.begin(map, management, waypoints);
                minecraft.setScreen(new GuiMap(management, management, session.getMapProcessor(), minecraft.player));
                return;
            }
        }
        minecraft.setScreen(new MapPreviewScreen(management, waypoints, map));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.literal("Regresar"), button -> onClose())
                .bounds(center - 154, this.height - 28, 100, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Eliminar"), button -> this.minecraft.setScreen(
                        new MapDeleteScreen(this, this.waypoints, this.map)))
                .bounds(center + 54, this.height - 28, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 28, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, this.map.server() + " — " + this.map.title(), this.width / 2, this.height / 2 - 10, 0xFFFFFF55);
        graphics.drawCenteredString(
                this.font,
                "Entra a ese mundo para ver el mapa en vivo.",
                this.width / 2,
                this.height / 2 + 8,
                0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.management);
    }
}
