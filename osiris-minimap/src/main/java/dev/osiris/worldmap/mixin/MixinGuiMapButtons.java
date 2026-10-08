package dev.osiris.worldmap.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.osiris.minimap.maps.MapPreview;
import dev.osiris.minimap.net.MapRules;
import dev.osiris.minimap.net.RemoteWaypoints;
import dev.osiris.minimap.maps.SavedMap;
import dev.osiris.minimap.maps.ui.MapDeleteScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.element.HoveredMapElementHolder;
import xaero.map.element.MapElementRenderHandler;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiMapSwitchingButton;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

/** Quita del mapa del mundo el engranaje, los claims, el cambio de dimensión y map switching. */
@Mixin(GuiMap.class)
public abstract class MixinGuiMapButtons extends Screen {
    protected MixinGuiMapButtons() {
        super(Component.empty());
    }
    @Shadow
    private Button dimensionToggleButton;

    @Shadow
    private Button settingsButton;

    @Shadow
    private Button claimsButton;

    @Shadow
    private Button caveModeButton;

    @Shadow
    private Button radarButton;

    @Shadow
    private Button playersButton;

    @Shadow
    public boolean playersMenu;

    @Shadow
    private Button hopButton;

    @Shadow
    private Button attachedCameraButton;

    @Shadow
    private EditBox hopInputBox;

    @Shadow
    protected abstract void removeWidget(GuiEventListener listener);

    /** Sube el salto a coordenadas y la cámara para centrarlos con la columna derecha. */
    private static final int SIDE_LIFT = 40;

    /** Valor de la interfaz oculta antes de esta previsualización, para devolverlo al salir del frame. */
    private boolean osiris$hiddenBefore;

    @Inject(method = "init", at = @At("RETURN"))
    private void osiris$hideMapButtons(CallbackInfo ci) {
        if (MapPreview.active()) {
            stripButtons();
            addPreviewButtons();
            return;
        }
        if (this.dimensionToggleButton != null) {
            this.removeWidget(this.dimensionToggleButton);
        }
        if (this.settingsButton != null) {
            this.removeWidget(this.settingsButton);
        }
        if (this.claimsButton != null) {
            this.removeWidget(this.claimsButton);
        }
        this.playersMenu = false;
        if (this.caveModeButton != null) {
            this.removeWidget(this.caveModeButton);
        }
        if (this.radarButton != null) {
            this.removeWidget(this.radarButton);
        }
        if (this.playersButton != null) {
            this.removeWidget(this.playersButton);
        }
        List<GuiEventListener> switching = new ArrayList<>();
        for (GuiEventListener child : ((Screen) (Object) this).children()) {
            if (child instanceof GuiMapSwitchingButton) {
                switching.add(child);
            }
        }
        switching.forEach(this::removeWidget);
        liftSideButton(this.attachedCameraButton);
        liftSideButton(this.hopButton);
        liftSideButton(this.hopInputBox);
    }

    private void liftSideButton(AbstractWidget widget) {
        if (widget != null) {
            widget.setY(widget.getY() - SIDE_LIFT);
        }
    }

    @Inject(method = "onDimensionToggleButton", at = @At("HEAD"), cancellable = true)
    private void osiris$blockDimensionToggle(Button button, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onSettingsButton", at = @At("HEAD"), cancellable = true)
    private void osiris$blockSettings(Button button, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onClaimsButton", at = @At("HEAD"), cancellable = true)
    private void osiris$blockClaims(Button button, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onCaveModeButton", at = @At("HEAD"), cancellable = true)
    private void osiris$blockCave(Button button, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "enableCaveModeOptions", at = @At("HEAD"), cancellable = true)
    private void osiris$blockCaveKey(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onRadarButton", at = @At("HEAD"), cancellable = true)
    private void osiris$blockRadar(Button button, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onPlayersButton", at = @At("HEAD"), cancellable = true)
    private void osiris$blockPlayers(Button button, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "togglePlayerMenu", at = @At("HEAD"), cancellable = true)
    private void osiris$blockPlayerMenu(CallbackInfo ci) {
        this.playersMenu = false;
        ci.cancel();
    }

    @Inject(method = "getRightClickOptions", at = @At("RETURN"))
    private void osiris$filterRightClick(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        ArrayList<RightClickOption> options = cir.getReturnValue();
        if (options == null || options.isEmpty()) {
            return;
        }
        if (MapPreview.active()) {
            options.clear();
            return;
        }
        boolean creative = creativePlayer();
        options.removeIf(option -> hideRightClick(((RightClickOptionAccessor) (Object) option).osiris$name(), creative));
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void osiris$leavePreview(CallbackInfo ci) {
        MapPreview.end();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void osiris$previewChromeOff(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        RemoteWaypoints.applyFrame();
        this.osiris$hiddenBefore = GuiMap.hiddenUI;
        if (MapPreview.active()) {
            GuiMap.hiddenUI = true;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void osiris$previewButtons(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!MapPreview.active()) {
            return;
        }
        for (GuiEventListener child : this.children()) {
            if (child instanceof Button button) {
                button.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        GuiMap.hiddenUI = this.osiris$hiddenBefore;
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void osiris$previewKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!MapPreview.active() || event.key() == 256) {
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void osiris$previewClick(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (MapPreview.active() && event.button() != 0) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/gui/GuiMap;drawArrowOnMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDFD)V"))
    private void osiris$skipArrow(GuiMap map, PoseStack pose, VertexConsumer buffer, double x, double z, float angle, double scale) {
        if (!MapPreview.active()) {
            map.drawArrowOnMap(pose, buffer, x, z, angle, scale);
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/gui/GuiMap;drawFarArrowOnMap(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDFD)V"))
    private void osiris$skipFarArrow(GuiMap map, PoseStack pose, VertexConsumer buffer, double x, double z, float angle, double scale) {
        if (!MapPreview.active()) {
            map.drawFarArrowOnMap(pose, buffer, x, z, angle, scale);
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/element/MapElementRenderHandler;render(Lxaero/map/gui/GuiMap;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;DDIIDDDDDFZLxaero/map/element/HoveredMapElementHolder;Lnet/minecraft/client/Minecraft;F)Lxaero/map/element/HoveredMapElementHolder;"))
    private HoveredMapElementHolder<?, ?> osiris$skipIcons(
            MapElementRenderHandler handler,
            GuiMap map,
            MultiBufferSource.BufferSource buffers,
            MultiTextureRenderTypeRendererProvider providers,
            double cameraX,
            double cameraZ,
            int caveStart,
            int caveDepth,
            double minX,
            double maxX,
            double minZ,
            double maxZ,
            double scale,
            float brightness,
            boolean cave,
            HoveredMapElementHolder<?, ?> hovered,
            Minecraft minecraft,
            float partialTick
    ) {
        if (MapPreview.active()) {
            return hovered;
        }
        return handler.render(
                map, buffers, providers, cameraX, cameraZ, caveStart, caveDepth, minX, maxX, minZ, maxZ, scale, brightness, cave, hovered, minecraft, partialTick);
    }

    private void stripButtons() {
        List<GuiEventListener> buttons = new ArrayList<>();
        for (GuiEventListener child : ((Screen) (Object) this).children()) {
            if (child instanceof Button) {
                buttons.add(child);
            }
        }
        buttons.forEach(this::removeWidget);
    }

    private void addPreviewButtons() {
        Screen screen = (Screen) (Object) this;
        int center = screen.width / 2;
        int y = screen.height - 28;
        this.addRenderableWidget(Button.builder(Component.literal("Regresar"), button -> {
                    Screen back = MapPreview.returnTo();
                    MapPreview.clear();
                    Minecraft.getInstance().setScreen(back);
                })
                .bounds(center - 154, y, 100, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Eliminar"), button -> {
                    SavedMap map = MapPreview.selected();
                    Screen waypoints = MapPreview.listParent();
                    MapPreview.keepForDelete();
                    Minecraft.getInstance().setScreen(new MapDeleteScreen(screen, waypoints, map));
                })
                .bounds(center + 54, y, 100, 20)
                .build());
    }

    private static boolean creativePlayer() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isCreative();
    }

    private static boolean hideRightClick(String name, boolean creative) {
        if (name == null) {
            return false;
        }
        if (name.contains("map_export") || name.contains("map_settings")) {
            return true;
        }
        if (!MapRules.waypoints() && name.contains("create_waypoint")) {
            return true;
        }
        return name.contains("teleport") && !creative;
    }
}
