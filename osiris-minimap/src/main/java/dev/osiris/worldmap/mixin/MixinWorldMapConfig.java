package dev.osiris.worldmap.mixin;

import dev.osiris.minimap.config.OsirisPaths;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;

@Mixin(WorldMap.class)
public class MixinWorldMapConfig {
    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/lib/common/config/channel/ConfigChannel$Builder;setConfigPath(Ljava/nio/file/Path;)Lxaero/lib/common/config/channel/ConfigChannel$Builder;"
            ),
            require = 0
    )
    private static Path osiris$clinitConfig(Path path) {
        return OsirisPaths.relocate(path, "osiris_worldmap");
    }

    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/lib/common/config/channel/ConfigChannel$Builder;setDefaultConfigsPath(Ljava/nio/file/Path;)Lxaero/lib/common/config/channel/ConfigChannel$Builder;"
            ),
            require = 0
    )
    private static Path osiris$clinitDefaults(Path path) {
        return OsirisPaths.relocate(path, "osiris_worldmap");
    }

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/lib/common/config/channel/ConfigChannel$Builder;setConfigPath(Ljava/nio/file/Path;)Lxaero/lib/common/config/channel/ConfigChannel$Builder;"
            ),
            require = 0
    )
    private static Path osiris$initConfig(Path path) {
        return OsirisPaths.relocate(path, "osiris_worldmap");
    }

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/lib/common/config/channel/ConfigChannel$Builder;setDefaultConfigsPath(Ljava/nio/file/Path;)Lxaero/lib/common/config/channel/ConfigChannel$Builder;"
            ),
            require = 0
    )
    private static Path osiris$initDefaults(Path path) {
        return OsirisPaths.relocate(path, "osiris_worldmap");
    }

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void osiris$fields(CallbackInfo ci) {
        relocateStaticPaths();
    }

    private static void relocateStaticPaths() {
        for (Field field : WorldMap.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = field.get(null);
                if (value instanceof Path path) {
                    field.set(null, OsirisPaths.relocate(path, "osiris_worldmap"));
                } else if (value instanceof File file) {
                    field.set(null, OsirisPaths.relocate(file.toPath(), "osiris_worldmap").toFile());
                }
            } catch (Exception ignored) {
                // Un campo final o no accesible de Xaero no debe tumbar el cliente.
            }
        }
    }
}
