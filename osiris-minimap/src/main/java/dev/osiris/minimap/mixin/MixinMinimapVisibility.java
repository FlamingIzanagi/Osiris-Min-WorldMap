package dev.osiris.minimap.mixin;

import dev.osiris.minimap.net.MapVisibility;
import dev.osiris.minimap.net.RemoteWaypoints;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.module.MinimapRenderer;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.module.ModuleSession;
import xaero.hud.render.module.ModuleRenderContext;

/** El servidor puede apagar el minimapa entero. El deslizamiento de rastreo se aplica antes de dibujar. */
@Mixin(MinimapRenderer.class)
public class MixinMinimapVisibility {
    @Inject(
            method = "render(Lxaero/hud/minimap/module/MinimapSession;Lxaero/hud/render/module/ModuleRenderContext;Lnet/minecraft/client/gui/GuiGraphics;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void osiris$hideMinimap(MinimapSession session, ModuleRenderContext context, GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        RemoteWaypoints.applyFrame();
        if (!MapVisibility.minimap()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lxaero/hud/module/ModuleSession;Lxaero/hud/render/module/ModuleRenderContext;Lnet/minecraft/client/gui/GuiGraphics;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void osiris$hideModule(ModuleSession<?> session, ModuleRenderContext context, GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        if (!MapVisibility.minimap()) {
            ci.cancel();
        }
    }
}
