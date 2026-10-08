package dev.osiris.minimap.mixin;

import dev.osiris.minimap.server.ServerIdentity;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** El login nuevo es la redirección. El respawn con la misma semilla es otra dimensión o una muerte. */
@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetwork {
    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void osiris$login(ClientboundLoginPacket packet, CallbackInfo ci) {
        ServerIdentity.onLogin(packet);
    }

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void osiris$respawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        ServerIdentity.onRespawn(packet);
    }
}
