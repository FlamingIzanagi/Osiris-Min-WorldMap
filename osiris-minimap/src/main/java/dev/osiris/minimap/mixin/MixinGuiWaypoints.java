package dev.osiris.minimap.mixin;

import dev.osiris.minimap.maps.ui.MapManagementScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.gui.GuiWaypoints;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

/**
 * La lista de waypoints no muestra servidor, dimensión, Options ni el filtro.
 * Teleport solo aparece en creativo. El set y los botones se reacomodan.
 */
@Mixin(GuiWaypoints.class)
public abstract class MixinGuiWaypoints extends Screen {
    protected MixinGuiWaypoints() {
        super(Component.empty());
    }
    /** Hueco de las filas de servidor y dimensión. */
    private static final int HEADER_SHIFT = 24;

    private static final int LIST_TOP = 58 - HEADER_SHIFT;

    private static final int SET_WIDTH = 200;
    private static final int TINY_WIDTH = 75;
    private static final int TINY_GAP = 8;

    private Button osiris$mapsButton;
    private int osiris$mapsX;
    private int osiris$mapsY;

    @Shadow
    private DropDownWidget containersDD;

    @Shadow
    private DropDownWidget worldsDD;

    @Shadow
    private DropDownWidget setsDD;

    @Shadow
    private Button deleteButton;

    @Shadow
    private Button editButton;

    @Shadow
    private Button teleportButton;

    @Shadow
    private Button disableEnableButton;

    @Shadow
    private Button clearButton;

    @Shadow
    private Button shareButton;

    @Shadow
    private EditBox filterBox;

    @Shadow
    public void clearFilter(boolean apply) {
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void osiris$layout(CallbackInfo ci) {
        layoutHeader();
        layoutActions();
        raiseList();
        addMapsButton();
    }

    @Inject(method = "onSelected", at = @At("RETURN"))
    private void osiris$keepLayout(DropDownWidget widget, int index, CallbackInfoReturnable<Boolean> cir) {
        layoutHeader();
    }

    @Inject(method = "updateButtons", at = @At("RETURN"))
    private void osiris$keepTeleport(CallbackInfo ci) {
        if (!creative() && this.teleportButton != null) {
            this.teleportButton.visible = false;
            this.teleportButton.active = false;
        }
    }

    @Inject(method = "canTeleport", at = @At("HEAD"), cancellable = true)
    private void osiris$creativeTeleport(CallbackInfoReturnable<Boolean> cir) {
        if (!creative()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "lambda$init$6", at = @At("HEAD"), cancellable = true)
    private void osiris$blockOptions(Button button, CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(
            method = "renderPreDropdown",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private void osiris$hideServerLabels(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
    }

    @ModifyConstant(method = "mouseClicked", constant = @Constant(doubleValue = 58.0))
    private double osiris$listDragTop(double original) {
        return LIST_TOP;
    }

    private void layoutHeader() {
        hide(this.containersDD);
        hide(this.worldsDD);
        Screen screen = (Screen) (Object) this;
        for (GuiEventListener child : screen.children()) {
            if (isOptions(child)) {
                hide((AbstractWidget) child);
            }
        }
        if (this.setsDD == null || this.clearButton == null) {
            return;
        }
        int center = screen.width / 2;
        int group = SET_WIDTH + (TINY_GAP + TINY_WIDTH) * 2;
        int setX = center - group / 2;
        int y = 32 - HEADER_SHIFT;
        this.setsDD.setX(setX);
        this.setsDD.setY(y + 1);
        this.clearButton.setX(setX + SET_WIDTH + TINY_GAP);
        this.clearButton.setY(y);
        this.osiris$mapsX = this.clearButton.getX() + TINY_WIDTH + TINY_GAP;
        this.osiris$mapsY = y;
        if (this.osiris$mapsButton != null) {
            this.osiris$mapsButton.setX(this.osiris$mapsX);
            this.osiris$mapsButton.setY(this.osiris$mapsY);
        }
        if (this.filterBox != null) {
            hide(this.filterBox);
            this.filterBox.setFocused(false);
            this.filterBox.setY(-1000);
            this.clearFilter(true);
        }
    }

    private void addMapsButton() {
        if (this.setsDD == null || this.clearButton == null) {
            return;
        }
        this.osiris$mapsButton = Button.builder(Component.literal("Mapas"), button -> Minecraft.getInstance()
                        .setScreen(new MapManagementScreen((Screen) (Object) this)))
                .bounds(this.osiris$mapsX, this.osiris$mapsY, TINY_WIDTH, 20)
                .build();
        this.addRenderableWidget(this.osiris$mapsButton);
    }

    private void layoutActions() {
        if (this.editButton == null || this.shareButton == null || this.disableEnableButton == null || this.deleteButton == null) {
            return;
        }
        if (creative()) {
            if (this.teleportButton != null) {
                this.teleportButton.visible = true;
            }
            return;
        }
        if (this.teleportButton != null) {
            this.teleportButton.visible = false;
            this.teleportButton.active = false;
        }
        int center = ((Screen) (Object) this).width / 2;
        int row = TINY_WIDTH * 4 + TINY_GAP * 3;
        int x = center - row / 2;
        int step = TINY_WIDTH + TINY_GAP;
        this.editButton.setX(x);
        this.shareButton.setX(x + step);
        this.disableEnableButton.setX(x + step * 2);
        this.deleteButton.setX(x + step * 3);
    }

    private void raiseList() {
        for (GuiEventListener child : ((Screen) (Object) this).children()) {
            if (child instanceof AbstractSelectionList<?> list) {
                list.updateSizeAndPosition(list.getWidth(), list.getHeight() + HEADER_SHIFT, LIST_TOP);
                return;
            }
        }
    }

    private static void hide(AbstractWidget widget) {
        if (widget == null) {
            return;
        }
        widget.visible = false;
        widget.active = false;
    }

    private static boolean isOptions(GuiEventListener child) {
        if (!(child instanceof Button button)) {
            return false;
        }
        return button.getMessage().getContents() instanceof TranslatableContents text
                && "gui.xaero_options".equals(text.getKey());
    }

    private static boolean creative() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.isCreative();
    }
}
