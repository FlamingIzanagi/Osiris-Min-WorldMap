package dev.osiris.minimap.mixin;

import dev.osiris.minimap.cave.Nether;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.config.util.MinimapConfigClientUtils;

/** Fuera del Nether el modo cueva queda cerrado. Dentro, Xaero puede calcular la capa automática. */
@Mixin(MinimapConfigClientUtils.class)
public class MixinMinimapCaveAllowed {
    @Inject(method = "getEffectiveCaveModeAllowed", at = @At("HEAD"), cancellable = true)
    private static void osiris$surface(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Nether.player());
    }
}
