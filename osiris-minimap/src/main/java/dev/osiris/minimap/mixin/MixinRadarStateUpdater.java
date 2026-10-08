package dev.osiris.minimap.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.radar.state.RadarStateUpdater;

/** El jugador se dibuja con la flecha del minimapa, no con el radar de entidades. */
@Mixin(RadarStateUpdater.class)
public class MixinRadarStateUpdater {
    @Inject(
            method = "update(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void osiris$skip(ClientLevel level, Entity renderView, Player player, CallbackInfo ci) {
        ci.cancel();
    }
}
