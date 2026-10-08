package dev.osiris.minimap.mixin;

import dev.osiris.minimap.brand.OsirisText;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Component.class)
public interface MixinComponentRebrand {
    @Inject(method = "getString()Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void osiris$rebrand(CallbackInfoReturnable<String> cir) {
        String value = cir.getReturnValue();
        String branded = OsirisText.rebrand(value);
        if (branded != value) {
            cir.setReturnValue(branded);
        }
    }
}
