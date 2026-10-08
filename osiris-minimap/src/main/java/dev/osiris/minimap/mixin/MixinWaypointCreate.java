package dev.osiris.minimap.mixin;

import dev.osiris.minimap.net.MapRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.controls.key.function.AddWaypointFunction;
import xaero.hud.minimap.controls.key.function.TemporaryWaypointFunction;

/** La tecla de crear waypoint y la de waypoint rápido respetan la regla del mundo actual. */
public final class MixinWaypointCreate {
    private MixinWaypointCreate() {
    }

    @Mixin(AddWaypointFunction.class)
    public static class Add {
        @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
        private void osiris$block(CallbackInfo ci) {
            if (!MapRules.waypoints()) {
                ci.cancel();
            }
        }
    }

    @Mixin(TemporaryWaypointFunction.class)
    public static class Temporary {
        @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
        private void osiris$block(CallbackInfo ci) {
            if (!MapRules.waypoints()) {
                ci.cancel();
            }
        }
    }
}
