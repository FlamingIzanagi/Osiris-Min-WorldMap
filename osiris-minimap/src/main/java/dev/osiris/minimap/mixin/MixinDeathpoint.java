package dev.osiris.minimap.mixin;

import dev.osiris.minimap.net.MapRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.waypoint.DeathpointHandler;

/** El punto de muerte solo se crea si el servidor lo permite en el mundo actual. */
@Mixin(DeathpointHandler.class)
public class MixinDeathpoint {
    @Inject(
            method = {
                    "createDeathpoint(Lnet/minecraft/world/entity/player/Player;)V",
                    "createDeathpoint(Lnet/minecraft/world/entity/player/Player;Lxaero/hud/minimap/world/MinimapWorld;Z)V"
            },
            at = @At("HEAD"),
            cancellable = true
    )
    private void osiris$blockDeathpoint(CallbackInfo ci) {
        if (!MapRules.deathpoints()) {
            ci.cancel();
        }
    }
}
