package dev.osiris.minimap.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("ALL")
    static Map<String, KeyMapping> osiris$all() {
        throw new AssertionError();
    }

    @Accessor("MAP")
    static Map<InputConstants.Key, List<KeyMapping>> osiris$byKey() {
        throw new AssertionError();
    }
}
