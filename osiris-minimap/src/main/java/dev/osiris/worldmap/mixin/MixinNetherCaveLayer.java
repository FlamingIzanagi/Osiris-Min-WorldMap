package dev.osiris.worldmap.mixin;

import dev.osiris.minimap.cave.Nether;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.world.MapDimension;

/** El tipo 0 es superficie. El 1 es la capa que sigue el techo sobre el jugador. */
@Mixin(MapDimension.class)
public class MixinNetherCaveLayer {
    @Inject(method = "getCaveModeType", at = @At("HEAD"), cancellable = true)
    private void osiris$nether(CallbackInfoReturnable<Integer> cir) {
        if (Nether.key(((MapDimension) (Object) this).getDimId())) {
            cir.setReturnValue(1);
        }
    }
}
