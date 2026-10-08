package dev.osiris.minimap.mixin;

import java.util.List;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** La lista pública {@code children()} es inmutable. Este campo es la lista real. */
@Mixin(AbstractSelectionList.class)
public interface SelectionChildrenAccessor {
    @Accessor("children")
    List<Object> osiris$children();
}
