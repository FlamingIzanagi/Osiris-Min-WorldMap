package dev.osiris.minimap.mixin;

import dev.osiris.minimap.controls.HiddenKeybinds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No redirige {@code Options.keyMappings}. FancyMenu y otros mods ya lo hacen,
 * y un segundo redirect sobre el mismo campo tira el arranque.
 * {@code children()} es una vista inmutable: se quita sobre la lista real.
 */
@Mixin(KeyBindsList.class)
public class MixinKeyBindsList {
    @Inject(
            method = "<init>(Lnet/minecraft/client/gui/screens/options/controls/KeyBindsScreen;Lnet/minecraft/client/Minecraft;)V",
            at = @At("RETURN")
    )
    private void osiris$hide(KeyBindsScreen screen, Minecraft minecraft, CallbackInfo ci) {
        List<KeyBindsList.Entry> children = ((KeyBindsList) (Object) this).children();
        List<KeyBindsList.Entry> drop = new ArrayList<>();
        KeyBindsList.Entry category = null;
        boolean kept = false;
        for (KeyBindsList.Entry entry : children) {
            if (entry instanceof KeyBindsList.CategoryEntry) {
                if (category != null && !kept) {
                    drop.add(category);
                }
                category = entry;
                kept = false;
                continue;
            }
            if (entry instanceof KeyBindsList.KeyEntry keyEntry) {
                var mapping = ((KeyBindsListKeyAccessor) keyEntry).osiris$key();
                if (mapping != null && HiddenKeybinds.hidesTranslation(mapping.getName())) {
                    drop.add(entry);
                    continue;
                }
            }
            kept = true;
        }
        if (category != null && !kept) {
            drop.add(category);
        }
        ((SelectionChildrenAccessor) (Object) this).osiris$children().removeAll(drop);
    }
}
