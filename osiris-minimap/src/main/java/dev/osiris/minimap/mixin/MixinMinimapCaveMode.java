package dev.osiris.minimap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.MinimapProcessor;

@Mixin(MinimapProcessor.class)
public class MixinMinimapCaveMode {
    @Inject(method = "isManualCaveMode", at = @At("HEAD"), cancellable = true)
    private void osiris$manual(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "toggleManualCaveMode", at = @At("HEAD"), cancellable = true)
    private void osiris$toggle(CallbackInfo ci) {
        ci.cancel();
    }
}
