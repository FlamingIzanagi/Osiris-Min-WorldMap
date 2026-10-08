package dev.osiris.worldmap.mixin;

import dev.osiris.minimap.cave.Nether;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.config.util.WorldMapClientConfigUtils;

/** Fuera del Nether el world map sigue en superficie. En el Nether se permite la capa de cueva. */
@Mixin(WorldMapClientConfigUtils.class)
public class MixinWorldMapCaveAllowed {
    @Inject(method = "getEffectiveCaveModeAllowed", at = @At("HEAD"), cancellable = true)
    private static void osiris$surface(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Nether.player());
    }
}
