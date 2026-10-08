package dev.osiris.minimap.mixin;

import dev.osiris.minimap.net.RemoteWaypoints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.render.WaypointDeleter;

/** El jugador no puede borrar un waypoint que puso el servidor. */
@Mixin(WaypointDeleter.class)
public class MixinWaypointLock {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void osiris$keepServerWaypoint(Waypoint waypoint, CallbackInfo ci) {
        if (RemoteWaypoints.locked(waypoint)) {
            ci.cancel();
        }
    }
}
