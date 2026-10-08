package dev.osiris.minimap.mixin;

import dev.osiris.minimap.server.SubServerPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.world.state.MinimapWorldState;
import xaero.hud.path.XaeroPath;

@Mixin(MinimapWorldState.class)
public class MixinWorldStatePaths {
    @Inject(method = "getAutoRootContainerPath", at = @At("RETURN"), cancellable = true)
    private void osiris$root(CallbackInfoReturnable<XaeroPath> cir) {
        cir.setReturnValue(SubServerPaths.apply(cir.getReturnValue()));
    }

    @Inject(method = "getAutoWorldPath", at = @At("RETURN"), cancellable = true)
    private void osiris$world(CallbackInfoReturnable<XaeroPath> cir) {
        cir.setReturnValue(SubServerPaths.apply(cir.getReturnValue()));
    }

    @Inject(method = "getCurrentContainerPath()Lxaero/hud/path/XaeroPath;", at = @At("RETURN"), cancellable = true)
    private void osiris$container(CallbackInfoReturnable<XaeroPath> cir) {
        cir.setReturnValue(SubServerPaths.apply(cir.getReturnValue()));
    }

    @Inject(method = "getCurrentWorldPath()Lxaero/hud/path/XaeroPath;", at = @At("RETURN"), cancellable = true)
    private void osiris$currentWorld(CallbackInfoReturnable<XaeroPath> cir) {
        cir.setReturnValue(SubServerPaths.apply(cir.getReturnValue()));
    }
}
