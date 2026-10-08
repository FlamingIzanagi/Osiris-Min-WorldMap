package dev.osiris.minimap.maps.ui;

import dev.osiris.minimap.maps.MapPreview;
import dev.osiris.minimap.maps.SavedMap;
import dev.osiris.minimap.maps.SavedMapStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Confirmación antes de borrar la carpeta del mapa. */
public class MapDeleteScreen extends Screen {
    private final Screen cancelTo;
    private final Screen waypoints;
    private final SavedMap map;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public MapDeleteScreen(Screen cancelTo, Screen waypoints, SavedMap map) {
        super(Component.literal("Eliminar mapa"));
        this.cancelTo = cancelTo;
        this.waypoints = waypoints;
        this.map = map;
    }

    @Override
    protected void init() {
        this.panelWidth = 300;
        this.panelHeight = 110;
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        int y = this.panelY + this.panelHeight - 28;
        this.addRenderableWidget(Button.builder(Component.literal("Sí, eliminar"), button -> confirm())
                .bounds(this.panelX + 16, y, 120, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("No"), button -> onClose())
                .bounds(this.panelX + this.panelWidth - 96, y, 80, 20)
                .build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);
        graphics.fill(this.panelX - 1, this.panelY - 1, this.panelX + this.panelWidth + 1, this.panelY, 0xFF373737);
        graphics.fill(this.panelX - 1, this.panelY + this.panelHeight, this.panelX + this.panelWidth + 1, this.panelY + this.panelHeight + 1, 0xFF373737);
        graphics.fill(this.panelX - 1, this.panelY, this.panelX, this.panelY + this.panelHeight, 0xFF373737);
        graphics.fill(this.panelX + this.panelWidth, this.panelY, this.panelX + this.panelWidth + 1, this.panelY + this.panelHeight, 0xFF373737);
        graphics.fill(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xF0101010);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, "¿Estás seguro de eliminar este mapa?", this.width / 2, this.panelY + 16, 0xFFFF5555);
        graphics.drawCenteredString(this.font, "No podrás recuperarlo de ninguna forma.", this.width / 2, this.panelY + 32, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, this.map.server() + " — " + this.map.title(), this.width / 2, this.panelY + 48, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        if (!(this.cancelTo instanceof xaero.map.gui.GuiMap) || !MapPreview.active()) {
            MapPreview.clear();
        }
        this.minecraft.setScreen(this.cancelTo);
    }

    private void confirm() {
        String error = SavedMapStore.delete(this.map);
        MapPreview.clear();
        if (error == null) {
            this.minecraft.setScreen(new MapManagementScreen(this.waypoints, "Mapa eliminado.", false));
        } else {
            this.minecraft.setScreen(new MapManagementScreen(this.waypoints, error, true));
        }
    }
}
