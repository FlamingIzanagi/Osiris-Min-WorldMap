package dev.osiris.minimap.mixin;

import dev.osiris.minimap.net.MapRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.gui.WaypointEditForm;
import xaero.hud.minimap.waypoint.WaypointVisibilityType;
import xaero.lib.client.gui.ScreenBase;
import xaero.lib.client.gui.widget.dropdown.DropDownWidget;

/**
 * El waypoint se guarda en el servidor y el set actuales. El formulario no muestra
 * el contenedor, el mundo, el set ni Reset, y la visibilidad solo alterna Local y Global.
 */
@Mixin(GuiAddWaypoint.class)
public class MixinGuiAddWaypoint {
    /** Hueco que ocupaban las dos filas de arriba (contenedor/mundo y Reset/set). */
    private static final int COMPACT_SHIFT = 44;

    @Shadow
    private boolean adding;

    @Shadow
    private Button resetButton;

    @Shadow
    private Button visibilityTypeButton;

    @Shadow
    private Button defaultVisibilityTypeButton;

    @Shadow
    private DropDownWidget setsDD;

    @Shadow
    private DropDownWidget containersDD;

    @Shadow
    private DropDownWidget worldsDD;

    @Shadow
    private WaypointEditForm getCurrent() {
        return null;
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void osiris$compactWaypointForm(CallbackInfo ci) {
        hide(this.containersDD);
        hide(this.worldsDD);
        hide(this.resetButton);
        hide(this.setsDD);
        for (GuiEventListener child : ((Screen) (Object) this).children()) {
            if (child instanceof AbstractWidget widget && widget.getY() >= 82) {
                widget.setY(widget.getY() - COMPACT_SHIFT);
            }
        }
        snapVisibility();
        if (this.adding && !MapRules.waypoints()) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                if (minecraft.screen == (Screen) (Object) this) {
                    ((ScreenBase) (Object) this).goBack();
                }
            });
        }
    }

    @Inject(method = "canConfirm", at = @At("HEAD"), cancellable = true)
    private void osiris$blockCreate(CallbackInfoReturnable<Boolean> cir) {
        if (this.adding && !MapRules.waypoints()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "lambda$init$13", at = @At("HEAD"), cancellable = true)
    private void osiris$cycleVisibility(Button button, CallbackInfo ci) {
        WaypointEditForm form = this.getCurrent();
        if (form == null) {
            ci.cancel();
            return;
        }
        WaypointVisibilityType next = form.visibilityType == WaypointVisibilityType.LOCAL
                ? WaypointVisibilityType.GLOBAL
                : WaypointVisibilityType.LOCAL;
        form.visibilityType = next;
        form.keepVisibilityType = false;
        if (this.visibilityTypeButton != null) {
            this.visibilityTypeButton.setMessage(next.getTranslation());
        }
        if (this.defaultVisibilityTypeButton != null) {
            this.defaultVisibilityTypeButton.active = true;
        }
        ci.cancel();
    }

    private static void hide(AbstractWidget widget) {
        if (widget == null) {
            return;
        }
        widget.visible = false;
        widget.active = false;
    }

    private void snapVisibility() {
        WaypointEditForm form = this.getCurrent();
        if (form == null || form.visibilityType == null) {
            return;
        }
        if (form.visibilityType == WaypointVisibilityType.LOCAL || form.visibilityType == WaypointVisibilityType.GLOBAL) {
            return;
        }
        form.visibilityType = form.visibilityType.isGlobal()
                ? WaypointVisibilityType.GLOBAL
                : WaypointVisibilityType.LOCAL;
        if (this.visibilityTypeButton != null) {
            this.visibilityTypeButton.setMessage(form.visibilityType.getTranslation());
        }
    }
}
