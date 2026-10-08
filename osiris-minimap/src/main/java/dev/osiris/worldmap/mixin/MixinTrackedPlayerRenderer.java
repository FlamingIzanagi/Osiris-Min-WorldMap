package dev.osiris.worldmap.mixin;

import com.mojang.blaze3d.textures.GpuTexture;
import dev.osiris.minimap.net.MapRules;
import dev.osiris.minimap.net.PlayerMarks;
import dev.osiris.minimap.render.EntityDot;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.graphics.MapRenderHelper;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.map.radar.tracker.PlayerTrackerMapElement;
import xaero.map.radar.tracker.PlayerTrackerMapElementRenderer;

@Mixin(PlayerTrackerMapElementRenderer.class)
public class MixinTrackedPlayerRenderer {
    private net.minecraft.client.renderer.MultiBufferSource.BufferSource osiris$buffers;
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void osiris$players(ElementRenderLocation location, boolean menu, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(MapRules.players() && !menu);
    }

    @Inject(
            method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void osiris$remember(
            PlayerTrackerMapElement<?> element,
            boolean hovered,
            double partialX,
            float angle,
            double partialY,
            double partialZ,
            xaero.map.element.render.ElementRenderInfo info,
            xaero.map.element.MapElementGraphics graphics,
            net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,
            xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider textures,
            CallbackInfoReturnable<Boolean> ci
    ) {
        this.osiris$buffers = buffers;
        if (!MapRules.fullTracking() && !PlayerMarks.loaded(element.getPlayerId())) {
            ci.setReturnValue(false);
            return;
        }
        PlayerMarks.rendering(element.getPlayerId());
    }

    /** El radar de entidades no dibuja jugadores. El icono del mapa no debe ocultarse por eso. */
    @Redirect(
            method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/radar/tracker/PlayerTrackerMapElement;wasRenderedOnRadar()Z"
            )
    )
    private boolean osiris$keepIcon(PlayerTrackerMapElement<?> element) {
        return false;
    }

    @Redirect(
            method = "renderElement(Lxaero/map/radar/tracker/PlayerTrackerMapElement;ZDFDDLxaero/map/element/render/ElementRenderInfo;Lxaero/map/element/MapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRendererProvider;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/graphics/MapRenderHelper;blitIntoMultiTextureRenderer(Lorg/joml/Matrix4f;Lxaero/map/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;FFIIIIIIFFFFIILcom/mojang/blaze3d/textures/GpuTexture;)V"
            )
    )
    private void osiris$icon(
            Matrix4f matrix,
            MultiTextureRenderTypeRenderer renderer,
            float x,
            float y,
            int u,
            int v,
            int width,
            int height,
            int regionWidth,
            int regionHeight,
            float red,
            float green,
            float blue,
            float alpha,
            int atlasWidth,
            int atlasHeight,
            GpuTexture texture
    ) {
        if (PlayerMarks.showHeads()) {
            MapRenderHelper.blitIntoMultiTextureRenderer(
                    matrix, renderer, x, y, u, v, width, height, regionWidth, regionHeight,
                    red, green, blue, alpha, atlasWidth, atlasHeight, texture);
            return;
        }
        if (this.osiris$buffers == null) {
            return;
        }
        EntityDot.worldMap(matrix, EntityDot.buffer(this.osiris$buffers));
    }
}
