package dev.osiris.worldmap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;
import xaero.map.radar.tracker.PlayerTrackerMenuRenderer;

@Mixin(PlayerTrackerMenuRenderer.class)
public class MixinTrackedPlayerMenu {
    @Inject(method = "onShowPlayersButton", at = @At("HEAD"), cancellable = true)
    private void osiris$button(GuiMap map, int mouseX, int mouseY, CallbackInfo ci) {
        ci.cancel();
    }
}
