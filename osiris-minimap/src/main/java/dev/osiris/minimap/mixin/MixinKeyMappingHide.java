package dev.osiris.minimap.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.osiris.minimap.controls.HiddenKeybinds;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public class MixinKeyMappingHide {
    @Inject(
            method = "<init>(Ljava/lang/String;ILnet/minecraft/client/KeyMapping$Category;)V",
            at = @At("RETURN")
    )
    private void osiris$hide(String translationKey, int code, KeyMapping.Category category, CallbackInfo ci) {
        hide(translationKey);
    }

    @Inject(
            method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILnet/minecraft/client/KeyMapping$Category;)V",
            at = @At("RETURN")
    )
    private void osiris$hideTyped(
            String translationKey,
            InputConstants.Type type,
            int code,
            KeyMapping.Category category,
            CallbackInfo ci
    ) {
        hide(translationKey);
    }

    @Inject(
            method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILnet/minecraft/client/KeyMapping$Category;I)V",
            at = @At("RETURN")
    )
    private void osiris$hideOrdered(
            String translationKey,
            InputConstants.Type type,
            int code,
            KeyMapping.Category category,
            int order,
            CallbackInfo ci
    ) {
        hide(translationKey);
    }

    private void hide(String translationKey) {
        if (HiddenKeybinds.hidesTranslation(translationKey)) {
            HiddenKeybinds.unbind((KeyMapping) (Object) this);
        }
    }
}
