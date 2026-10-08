package dev.osiris.worldmap.mixin;

import dev.osiris.minimap.server.ServerIdentity;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;

@Mixin(MapProcessor.class)
public class MixinMapProcessorWorldId {
    @Inject(
            method = "getMainId(ILnet/minecraft/client/multiplayer/ClientPacketListener;)Ljava/lang/String;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void osiris$main(int format, ClientPacketListener listener, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ServerIdentity.folder(cir.getReturnValue()));
    }

    @Inject(method = "getCurrentWorldId", at = @At("RETURN"), cancellable = true)
    private void osiris$current(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(ServerIdentity.folder(cir.getReturnValue()));
    }
}
