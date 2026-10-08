package dev.osiris.minimap.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import xaero.common.graphics.CustomRenderTypes;
import xaero.hud.render.util.RenderBufferUtil;

/**
 * El punto del radar de entidades de Xaero: el círculo de la textura gui, no un cuadrado pintado.
 * En el minimapa se achica; en el world map se deja al tamaño del sprite.
 */
public final class EntityDot {
    private static final float MINIMAP_SCALE = 0.5F;
    private static final float WORLD_MAP_SCALE = 1.0F;

    private EntityDot() {
    }

    public static void minimap(Matrix4f matrix, VertexConsumer consumer) {
        draw(matrix, consumer, MINIMAP_SCALE);
    }

    public static void worldMap(Matrix4f matrix, VertexConsumer consumer) {
        draw(matrix, consumer, WORLD_MAP_SCALE);
    }

    public static VertexConsumer buffer(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers) {
        return buffers.getBuffer(CustomRenderTypes.GUI_BILINEAR);
    }

    private static void draw(Matrix4f matrix, VertexConsumer consumer, float scale) {
        Matrix4f scaled = new Matrix4f(matrix);
        scaled.scale(scale, scale, 1.0F);
        RenderBufferUtil.addTexturedColoredRect(scaled, consumer, -5.5F, -5.5F, 0, 117, 11, 11, 1.0F, 1.0F, 1.0F, 1.0F, 256.0F);
    }
}
