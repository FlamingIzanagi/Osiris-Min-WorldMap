package dev.osiris.minimap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

@Mixin(MinimapElementRenderer.class)
public interface MinimapContextAccessor {
    @Accessor("context")
    Object osiris$context();
}
