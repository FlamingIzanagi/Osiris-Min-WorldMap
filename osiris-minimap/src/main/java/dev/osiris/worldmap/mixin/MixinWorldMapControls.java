package dev.osiris.worldmap.mixin;

import dev.osiris.minimap.controls.HiddenKeybinds;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xaero.map.controls.ControlsRegister;

@Mixin(ControlsRegister.class)
public class MixinWorldMapControls {
    @ModifyVariable(
            method = "register(Ljava/util/function/Consumer;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private Consumer<KeyMapping> osiris$filter(Consumer<KeyMapping> registrar) {
        return mapping -> {
            if (mapping != null && HiddenKeybinds.hidesTranslation(mapping.getName())) {
                HiddenKeybinds.unbind(mapping);
                return;
            }
            registrar.accept(mapping);
        };
    }
}
