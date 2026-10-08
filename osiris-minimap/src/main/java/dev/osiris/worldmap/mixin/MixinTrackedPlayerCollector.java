package dev.osiris.worldmap.mixin;

import dev.osiris.minimap.net.MapRules;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.radar.tracker.PlayerTrackerMapElementCollector;

@Mixin(PlayerTrackerMapElementCollector.class)
public class MixinTrackedPlayerCollector {
    @Inject(method = "update(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"), cancellable = true)
    private void osiris$skip(Minecraft minecraft, CallbackInfo ci) {
        if (!MapRules.players()) {
            ci.cancel();
        }
    }
}
