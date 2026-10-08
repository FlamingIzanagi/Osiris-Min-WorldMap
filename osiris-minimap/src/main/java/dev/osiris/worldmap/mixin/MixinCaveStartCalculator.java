package dev.osiris.worldmap.mixin;

import dev.osiris.minimap.cave.Nether;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.lib.client.config.ClientConfigManager;
import xaero.lib.common.config.option.ConfigOption;
import xaero.map.misc.CaveStartCalculator;

/** {@link Integer#MAX_VALUE} es la superficie. En el Nether se deja el cálculo automático. */
@Mixin(CaveStartCalculator.class)
public class MixinCaveStartCalculator {
    @Inject(
            method = "getCaving(DDDLnet/minecraft/world/level/Level;)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void osiris$surface(double x, double y, double z, Level level, CallbackInfoReturnable<Integer> cir) {
        if (Nether.level(level)) {
            return;
        }
        cir.setReturnValue(Integer.MAX_VALUE);
    }

    /** Con el ajuste en 0, Xaero no busca techo. En el Nether un 2 recorre el entorno del jugador. */
    @Redirect(
            method = "getCaving(DDDLnet/minecraft/world/level/Level;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/lib/client/config/ClientConfigManager;getEffective(Lxaero/lib/common/config/option/ConfigOption;)Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private Object osiris$autoCave(ClientConfigManager manager, ConfigOption<?> option) {
        Object value = manager.getEffective(option);
        if (value instanceof Integer number && number == 0) {
            return 2;
        }
        return value;
    }
}
