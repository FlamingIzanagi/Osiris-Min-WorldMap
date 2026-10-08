package dev.osiris.minimap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.controls.key.function.ManualCaveModeFunction;

@Mixin(ManualCaveModeFunction.class)
public class MixinManualCaveModeFunction {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void osiris$press(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onRelease", at = @At("HEAD"), cancellable = true)
    private void osiris$release(CallbackInfo ci) {
        ci.cancel();
    }
}
