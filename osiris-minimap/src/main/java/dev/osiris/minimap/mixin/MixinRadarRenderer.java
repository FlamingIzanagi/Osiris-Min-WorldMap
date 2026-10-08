package dev.osiris.minimap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.radar.render.element.RadarRenderer;

@Mixin(RadarRenderer.class)
public class MixinRadarRenderer {
    @Inject(
            method = "shouldRender(Lxaero/hud/minimap/element/render/MinimapElementRenderLocation;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void osiris$off(MinimapElementRenderLocation location, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
