package dev.osiris.minimap.mixin;

import dev.osiris.minimap.brand.OsirisText;
import dev.osiris.minimap.brand.SpanishUi;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
public class MixinClientLanguageRebrand {
    @Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void osiris$rebrand(String key, String fallback, CallbackInfoReturnable<String> cir) {
        String spanish = SpanishUi.translate(key);
        if (spanish != null) {
            cir.setReturnValue(OsirisText.rebrand(spanish));
            return;
        }
        String value = cir.getReturnValue();
        String branded = OsirisText.rebrand(value);
        if (branded != value) {
            cir.setReturnValue(branded);
        }
    }
}
