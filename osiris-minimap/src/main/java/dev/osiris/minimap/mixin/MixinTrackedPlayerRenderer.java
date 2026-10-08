package dev.osiris.minimap.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.osiris.minimap.net.MapRules;
import dev.osiris.minimap.net.PlayerMarks;
import dev.osiris.minimap.render.EntityDot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRenderer;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.player.tracker.PlayerTrackerIconRenderer;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElement;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementRenderer;
import xaero.hud.render.util.RenderBufferUtil;

@Mixin(PlayerTrackerMinimapElementRenderer.class)
public class MixinTrackedPlayerRenderer {
    private net.minecraft.client.renderer.MultiBufferSource.BufferSource osiris$buffers;
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void osiris$players(MinimapElementRenderLocation location, CallbackInfoReturnable<Boolean> cir) {
        if (!MapRules.players() || location == MinimapElementRenderLocation.IN_WORLD) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(
            method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void osiris$remember(
            PlayerTrackerMinimapElement<?> element,
            boolean hovered,
            boolean names,
            double partialX,
            float angle,
            double partialY,
            double partialZ,
            xaero.hud.minimap.element.render.MinimapElementRenderInfo info,
            xaero.hud.minimap.element.render.MinimapElementGraphics graphics,
            net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci
    ) {
        this.osiris$buffers = buffers;
        if (!MapRules.fullTracking() && !PlayerMarks.loaded(element.getPlayerId())) {
            ci.setReturnValue(false);
            return;
        }
        PlayerMarks.rendering(element.getPlayerId());
    }

    /** Si el radar marcó al jugador, Xaero no dibuja el icono. El radar de entidades está apagado. */
    @Redirect(
            method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;wasRenderedOnRadar()Z"
            )
    )
    private boolean osiris$keepIcon(PlayerTrackerMinimapElement<?> element) {
        return false;
    }

    /** Por debajo de 10 bloques Xaero no dibuja el icono. Esa distancia solo servía para dejar sitio al radar. */
    @Redirect(
            method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;sqrt(D)D")
    )
    private double osiris$keepClose(double squared) {
        double distance = Math.sqrt(squared);
        return distance < 10.0 ? 10.0 : distance;
    }

    @Redirect(
            method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/render/util/RenderBufferUtil;addColoredRect(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFIIFFFF)V",
                    ordinal = 0
            )
    )
    private void osiris$plate(Matrix4f matrix, com.mojang.blaze3d.vertex.VertexConsumer consumer, float x, float y, int width, int height, float red, float green, float blue, float alpha) {
        if (!PlayerMarks.showHeads()) {
            return;
        }
        RenderBufferUtil.addColoredRect(matrix, consumer, x, y, width, height, red, green, blue, alpha);
    }

    @Redirect(
            method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lxaero/hud/minimap/element/render/MinimapElementGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/minimap/player/tracker/PlayerTrackerIconRenderer;renderIcon(Lnet/minecraft/client/Minecraft;Lxaero/common/graphics/renderer/multitexture/MultiTextureRenderTypeRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/multiplayer/PlayerInfo;F)V"
            )
    )
    private void osiris$icon(
            PlayerTrackerIconRenderer renderer,
            Minecraft minecraft,
            MultiTextureRenderTypeRenderer textures,
            PoseStack pose,
            Player player,
            Identifier skin,
            PlayerInfo info,
            float opacity
    ) {
        if (PlayerMarks.showHeads()) {
            renderer.renderIcon(minecraft, textures, pose, player, skin, info, opacity);
            return;
        }
        if (this.osiris$buffers == null) {
            return;
        }
        EntityDot.minimap(pose.last().pose(), EntityDot.buffer(this.osiris$buffers));
    }
}
