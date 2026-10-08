package dev.osiris.worldmap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.element.render.ElementRenderer;

@Mixin(ElementRenderer.class)
public interface MapContextAccessor {
    @Accessor("context")
    Object osiris$context();
}
