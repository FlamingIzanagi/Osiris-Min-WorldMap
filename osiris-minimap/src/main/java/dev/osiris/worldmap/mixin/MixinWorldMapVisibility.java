package dev.osiris.worldmap.mixin;

import dev.osiris.minimap.net.MapVisibility;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.controls.ControlsHandler;
import xaero.map.controls.ControlsRegister;
import xaero.map.events.ClientEvents;
import xaero.map.gui.GuiMap;

/** Con el world map apagado, la tecla M no abre la pantalla y un intento tardío tampoco la deja. */
public final class MixinWorldMapVisibility {
    private MixinWorldMapVisibility() {
    }

    @Mixin(ControlsHandler.class)
    public static class Keys {
        @Inject(method = "keyDown", at = @At("HEAD"), cancellable = true)
        private void osiris$blockOpen(KeyMapping key, boolean tickEnd, boolean isRepeat, CallbackInfo ci) {
            if (!MapVisibility.worldMap() && key == ControlsRegister.keyOpenMap) {
                ci.cancel();
            }
        }
    }

    @Mixin(ClientEvents.class)
    public static class Open {
        @Inject(method = "handleGuiOpen", at = @At("RETURN"), cancellable = true)
        private void osiris$blockScreen(Screen screen, CallbackInfoReturnable<Screen> cir) {
            if (cir.getReturnValue() instanceof GuiMap && !MapVisibility.worldMap()) {
                cir.setReturnValue(Minecraft.getInstance().screen);
            }
        }
    }
}
