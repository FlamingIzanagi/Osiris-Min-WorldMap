package dev.osiris.worldmap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

@Mixin(RightClickOption.class)
public interface RightClickOptionAccessor {
    @Accessor("name")
    String osiris$name();
}
