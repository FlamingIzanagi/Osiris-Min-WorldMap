package dev.osiris.minimap.cave;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** El Nether es la única dimensión donde el mapa usa el modo cueva automático. */
public final class Nether {
    private Nether() {
    }

    public static boolean level(Level level) {
        return level != null && key(level.dimension());
    }

    public static boolean player() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && level(minecraft.level);
    }

    public static boolean key(ResourceKey<Level> dimension) {
        return dimension != null && "minecraft:the_nether".equals(dimension.identifier().toString());
    }
}
