package dev.osiris.minimap.maps.ui;

import dev.osiris.minimap.maps.MapPreview;
import dev.osiris.minimap.maps.SavedMap;
import dev.osiris.minimap.maps.SavedMapStore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Lista los mapas guardados, agrupados por servidor. */
public class MapManagementScreen extends net.minecraft.client.gui.screens.Screen {
    private final net.minecraft.client.gui.screens.Screen parent;
    private final Component status;
    private final boolean statusError;
    private EditBox search;
    private MapList list;
    private List<SavedMap> maps = List.of();

    public MapManagementScreen(net.minecraft.client.gui.screens.Screen parent) {
        this(parent, null, false);
    }

    public MapManagementScreen(net.minecraft.client.gui.screens.Screen parent, String status, boolean statusError) {
        super(Component.literal("Gestión de Mapas"));
        this.parent = parent;
        this.status = status == null ? null : Component.literal(status);
        this.statusError = statusError;
    }

    @Override
    protected void init() {
        // GuiMap vuelve a inicializar esta pantalla como padre. Eso no es abrir el menú.
        if (this.minecraft != null && this.minecraft.screen == this) {
            MapPreview.clear();
        }
        this.maps = SavedMapStore.load();
        int center = this.width / 2;
        this.search = new EditBox(this.font, 20, 32, 160, 20, Component.literal("Buscar..."));
        this.search.setHint(Component.literal("Buscar..."));
        this.search.setMaxLength(64);
        this.search.setResponder(text -> refill());
        this.addRenderableWidget(this.search);

        int top = 56;
        int bottom = this.height - 36;
        this.list = new MapList(this.minecraft, this.width, Math.max(40, bottom - top), top, 24);
        this.list.setX(0);
        this.addRenderableWidget(this.list);
        refill();

        this.addRenderableWidget(Button.builder(Component.literal("Regresar"), button -> onClose())
                .bounds(center - 208, this.height - 28, 100, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Hecho"), button -> this.minecraft.setScreen(null))
                .bounds(center - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
        if (this.status != null) {
            graphics.drawCenteredString(this.font, this.status, this.width / 2, this.height - 46, this.statusError ? 0xFFFF5555 : 0xFF55FF55);
        }
        if (this.list.children().isEmpty()) {
            String empty = this.maps.isEmpty() ? "No hay mapas guardados." : "Ningún mapa coincide.";
            graphics.drawCenteredString(this.font, empty, this.width / 2, this.height / 2, 0xFFA0A0A0);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private void refill() {
        String query = this.search.getValue().trim().toLowerCase();
        List<MapList.Row> rows = new ArrayList<>();
        String lastServer = null;
        for (SavedMap map : this.maps) {
            if (!query.isEmpty()
                    && !map.server().toLowerCase().contains(query)
                    && !map.title().toLowerCase().contains(query)) {
                continue;
            }
            if (!map.server().equals(lastServer)) {
                rows.add(this.list.new HeaderRow(map.server()));
                lastServer = map.server();
            }
            rows.add(this.list.new MapRow(map));
        }
        this.list.replaceEntries(rows);
    }

    private final class MapList extends AbstractSelectionList<MapList.Row> {
        private MapList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            this.centerListVertically = false;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

        @Override
        public int getRowWidth() {
            return Math.min(440, this.width - 40);
        }

        private abstract class Row extends Entry<Row> {
            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            }
        }

        private final class HeaderRow extends Row {
            private final String server;

            private HeaderRow(String server) {
                this.server = server;
                this.setHeight(16);
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
                graphics.drawString(
                        MapManagementScreen.this.font,
                        this.server,
                        this.getContentX() + 4,
                        this.getContentY() + 4,
                        0xFFFFFF55);
            }
        }

        private final class MapRow extends Row {
            private final SavedMap map;
            private final Button preview;
            private final Button delete;

            private MapRow(SavedMap map) {
                this.map = map;
                this.setHeight(24);
                this.preview = Button.builder(Component.literal("Preview"), button -> openPreview()).bounds(0, 0, 72, 20).build();
                this.delete = Button.builder(Component.literal("Eliminar"), button -> openDelete()).bounds(0, 0, 72, 20).build();
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
                place();
                graphics.drawString(
                        MapManagementScreen.this.font,
                        trim(this.map.title(), this.preview.getX() - this.getContentX() - 8),
                        this.getContentX() + 4,
                        this.getContentY() + 6,
                        0xFFFFFFFF);
                this.preview.render(graphics, mouseX, mouseY, partialTick);
                this.delete.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
                place();
                return this.preview.mouseClicked(event, doubled) || this.delete.mouseClicked(event, doubled);
            }

            @Override
            public boolean mouseReleased(MouseButtonEvent event) {
                place();
                return this.preview.mouseReleased(event) || this.delete.mouseReleased(event);
            }

            @Override
            public void visitWidgets(java.util.function.Consumer<net.minecraft.client.gui.components.AbstractWidget> consumer) {
                consumer.accept(this.preview);
                consumer.accept(this.delete);
            }

            private void place() {
                int deleteX = this.getContentRight() - 72;
                int previewX = deleteX - 76;
                int y = this.getContentY() + 2;
                this.preview.setPosition(previewX, y);
                this.delete.setPosition(deleteX, y);
            }

            private void openPreview() {
                MapPreviewScreen.open(MapManagementScreen.this, MapManagementScreen.this.parent, this.map);
            }

            private void openDelete() {
                MapManagementScreen.this.minecraft.setScreen(
                        new MapDeleteScreen(MapManagementScreen.this, MapManagementScreen.this.parent, this.map));
            }

            private String trim(String text, int maxWidth) {
                if (maxWidth < 20) {
                    return "";
                }
                if (MapManagementScreen.this.font.width(text) <= maxWidth) {
                    return text;
                }
                FormattedCharSequence sequence = MapManagementScreen.this.font.split(Component.literal(text), maxWidth).getFirst();
                StringBuilder cut = new StringBuilder();
                sequence.accept((index, style, codePoint) -> {
                    cut.appendCodePoint(codePoint);
                    return true;
                });
                return cut + "...";
            }
        }
    }
}
