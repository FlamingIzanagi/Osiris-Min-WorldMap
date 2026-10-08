package dev.osiris.minimap.mixin;

import dev.osiris.minimap.server.SubServerPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.world.state.MinimapWorldStateUpdater;
import xaero.hud.path.XaeroPath;

@Mixin(MinimapWorldStateUpdater.class)
public class MixinAutoRootPath {
    @Inject(method = "getAutoRootContainerPath(I)Lxaero/hud/path/XaeroPath;", at = @At("RETURN"), cancellable = true)
    private void osiris$sub(int format, CallbackInfoReturnable<XaeroPath> cir) {
        cir.setReturnValue(SubServerPaths.apply(cir.getReturnValue()));
    }
}
