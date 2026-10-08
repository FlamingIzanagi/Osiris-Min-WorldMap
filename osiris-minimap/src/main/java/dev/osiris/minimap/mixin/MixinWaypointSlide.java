package dev.osiris.minimap.mixin;

import dev.osiris.minimap.net.RemoteWaypoints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.render.WaypointMapRenderer;
import xaero.hud.minimap.waypoint.render.world.WaypointWorldRenderer;

/**
 * Xaero dibuja el icono en el centro del bloque. La fracción del rastreo se suma a esa coordenada
 * para que el icono se deslice entre actualizaciones del servidor.
 */
@Mixin({WaypointMapRenderer.class, WaypointWorldRenderer.class})
public class MixinWaypointSlide {
    @Inject(method = "preRender", at = @At("HEAD"))
    private void osiris$beforeFrame(CallbackInfo ci) {
        RemoteWaypoints.applyFrame();
    }

    @ModifyVariable(
            method = "renderElement(Lxaero/common/minimap/waypoints/Waypoint;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At("STORE"),
            index = 16
    )
    private double osiris$slideX(double stored, Waypoint waypoint) {
        return RemoteWaypoints.slideX(waypoint, stored);
    }

    @ModifyVariable(
            method = "renderElement(Lxaero/common/minimap/waypoints/Waypoint;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At("STORE"),
            index = 18
    )
    private double osiris$slideZ(double stored, Waypoint waypoint) {
        return RemoteWaypoints.slideZ(waypoint, stored);
    }
}
