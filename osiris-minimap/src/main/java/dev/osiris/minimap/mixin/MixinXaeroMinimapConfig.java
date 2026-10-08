package dev.osiris.minimap.mixin;

import dev.osiris.minimap.config.OsirisPaths;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.minimap.XaeroMinimap;

@Mixin(XaeroMinimap.class)
public class MixinXaeroMinimapConfig {
    @Inject(method = "getConfigSubFolder", at = @At("RETURN"), cancellable = true)
    private void osiris$config(CallbackInfoReturnable<Path> cir) {
        cir.setReturnValue(OsirisPaths.relocate(cir.getReturnValue(), "osiris_minimap"));
    }

    @Inject(method = "getDefaultConfigsSubFolder", at = @At("RETURN"), cancellable = true)
    private void osiris$defaults(CallbackInfoReturnable<Path> cir) {
        cir.setReturnValue(OsirisPaths.relocate(cir.getReturnValue(), "osiris_minimap"));
    }

    @Inject(method = "getOldConfigFileName", at = @At("RETURN"), cancellable = true)
    private void osiris$legacy(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("osiris_minimap.txt");
    }

    @Inject(method = "getCommonConfigFileName", at = @At("RETURN"), cancellable = true)
    private void osiris$common(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("osiris_minimap-common.txt");
    }
}
