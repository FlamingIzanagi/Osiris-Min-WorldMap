package dev.osiris.minimap.mixin;

import dev.osiris.minimap.controls.HiddenKeybinds;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.controls.key.KeyMappingControllerManager;
import xaero.hud.controls.key.function.KeyMappingFunction;

@Mixin(KeyMappingControllerManager.class)
public class MixinKeyMappingController {
    @Inject(
            method = "registerController(Lnet/minecraft/client/KeyMapping;ZLjava/util/function/Consumer;)V",
            at = @At("RETURN")
    )
    private void osiris$hide(KeyMapping mapping, boolean keyDown, Consumer<KeyMapping> registrar, CallbackInfo ci) {
        if (mapping != null && HiddenKeybinds.hidesTranslation(mapping.getName())) {
            HiddenKeybinds.unbind(mapping);
        }
    }

    @Inject(
            method = "registerFunction(Lnet/minecraft/client/KeyMapping;Lxaero/hud/controls/key/function/KeyMappingFunction;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void osiris$noFunction(KeyMapping mapping, KeyMappingFunction function, CallbackInfo ci) {
        if (mapping != null && HiddenKeybinds.hidesTranslation(mapping.getName())) {
            ci.cancel();
        }
    }
}
