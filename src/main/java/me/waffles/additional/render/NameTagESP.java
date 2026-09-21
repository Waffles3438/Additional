package me.waffles.additional.render;

import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.platform.GLX;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import java.nio.FloatBuffer;
import me.waffles.additional.config.ModConfig;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Draws labels missed by the normal entity pass, using that pass's camera and frustum. */
public final class NameTagESP {
    public static final Set<UUID> renderedPlayers = new HashSet<>();

    private NameTagESP() {}

    public static void beginFrame() { renderedPlayers.clear(); }

    @SuppressWarnings("unchecked")
    public static void render(Entity camera, Culler culler, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!ModConfig.masterSwitch || !ModConfig.nametagsThroughWalls || mc.world == null || mc.player == null) return;
        double cx = camera.prevX + (camera.x - camera.prevX) * tickDelta;
        double cy = camera.prevY + (camera.y - camera.prevY) * tickDelta;
        double cz = camera.prevZ + (camera.z - camera.prevZ) * tickDelta;
        RenderState previous = null;
        try {
            for (PlayerEntity player : mc.world.players) {
                if (player == mc.player || player.removed || player.deathTicks > 0 || !player.inChunk
                        || renderedPlayers.contains(player.getUuid()) || BotUtils.isBot(player)
                        || !culler.isVisible(player.getShape())) continue;
                EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().getRenderer(player);
                if (!(renderer instanceof LivingEntityRenderer)) continue;
                double x = player.prevX + (player.x - player.prevX) * tickDelta - cx;
                double y = player.prevY + (player.y - player.prevY) * tickDelta - cy;
                double z = player.prevZ + (player.z - player.prevZ) * tickDelta - cz;
                if (previous == null) previous = new RenderState();
                int light = player.isOnFire() ? 15728880 : player.getLightLevel(tickDelta);
                GLX.multiTexCoord2f(GLX.GL_TEXTURE1, light % 65536, light / 65536);
                GlStateManager.pushMatrix();
                try {
                    ((LivingEntityRenderer<PlayerEntity>) renderer).renderNameTag(player, x, y, z);
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        } finally {
            if (previous != null) previous.restore();
        }
    }

    /** Restore through GlStateManager so its cache agrees with OpenGL. */
    private static final class RenderState {
        private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final FloatBuffer color = BufferUtils.createFloatBuffer(4);
        private final FloatBuffer light = BufferUtils.createFloatBuffer(4);

        private RenderState() {
            GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, color);
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            GlStateManager.activeTexture(GLX.GL_TEXTURE1);
            GL11.glGetFloatv(GL11.GL_CURRENT_TEXTURE_COORDS, light);
            GlStateManager.activeTexture(activeTexture);
        }

        private void restore() {
            GLX.multiTexCoord2f(GLX.GL_TEXTURE1, light.get(0), light.get(1));
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (depth) GlStateManager.enableDepthTest(); else GlStateManager.disableDepthTest();
            if (texture) GlStateManager.enableTexture(); else GlStateManager.disableTexture();
            GlStateManager.depthMask(depthMask);
            // Text rendering can change the driver alpha factors via blendFunc.
            GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GlStateManager.color4f(color.get(0), color.get(1), color.get(2), color.get(3));
        }
    }
}
