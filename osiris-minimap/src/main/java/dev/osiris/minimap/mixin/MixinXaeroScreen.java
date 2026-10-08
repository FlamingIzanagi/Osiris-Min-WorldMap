package dev.osiris.minimap.mixin;

import dev.osiris.minimap.ui.OsirisWidgets;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinXaeroScreen {
    @Shadow
    protected abstract void removeWidget(GuiEventListener listener);

    @Inject(method = "init()V", at = @At("RETURN"))
    private void osiris$init(CallbackInfo ci) {
        OsirisWidgets.hiddenWidgets((Screen) (Object) this).forEach(this::removeWidget);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void osiris$tick(CallbackInfo ci) {
        OsirisWidgets.hiddenWidgets((Screen) (Object) this).forEach(this::removeWidget);
    }
}
