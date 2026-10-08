package me.waffles.additional.render;

import me.waffles.additional.config.ModConfig;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.FrustumCuller;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.platform.GLX;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class NameTagESP {
    private final Minecraft mc = Minecraft.getInstance();
    private final List<PlayerEntity> candidates = new ArrayList<>(16);
    private final RenderState previous = new RenderState();
    private Culler frustum;
    private static boolean renderingEsp;

    public static boolean isRenderingEsp() { return renderingEsp; }

    public static boolean shouldDefer(Entity entity) {
        return !renderingEsp && ModConfig.masterSwitch && ModConfig.nametagsThroughWalls
                && entity instanceof PlayerEntity && !BotUtils.isBot(entity);
    }

    @SuppressWarnings("unchecked")
    public void onRenderWorld(float tickDelta) {
        if (!ModConfig.masterSwitch || !ModConfig.nametagsThroughWalls) return;
        PlayerEntity viewer = mc.player;
        if (viewer == null || mc.world == null) return;

        candidates.clear();
        Entity camera = mc.getCamera();
        if (camera == null) camera = viewer;
        double px = camera.prevX + (camera.x - camera.prevX) * tickDelta;
        double py = camera.prevY + (camera.y - camera.prevY) * tickDelta;
        double pz = camera.prevZ + (camera.z - camera.prevZ) * tickDelta;

        boolean extendedRange = ModConfig.extendNametagRange;
        boolean showSneaking = ModConfig.nametagsOnShift;
        boolean frustumPrepared = false;
        List<PlayerEntity> players = mc.world.players;
        for (int i = 0, count = players.size(); i < count; i++) {
            PlayerEntity player = players.get(i);
            if (player == viewer || player.removed || player.deathTicks > 0 || !player.inChunk) continue;
            // Chunk membership and death animation state prevent stale, shaking labels.
            // Health is deliberately omitted: servers can report fake health for players.
            // Match the living renderer's range before bot, frustum and GL work.
            double rangeSquared = extendedRange ? 65536.0
                    : (!showSneaking && player.isSneaking() ? 1024.0 : 4096.0);
            if (player.squaredDistanceTo(camera) >= rangeSquared || BotUtils.isBot(player)) continue;
            if (!frustumPrepared) {
                if (frustum == null) frustum = new FrustumCuller();
                else Frustum.getInstance();
                frustum.prepare(px, py, pz);
                frustumPrepared = true;
            }
            if (frustum.isVisible(player.getShape())) candidates.add(player);
        }
        if (candidates.isEmpty()) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.prepare(mc.world, mc.textRenderer, camera, mc.targetEntity, mc.options, tickDelta);
        previous.capture();
        GlStateManager.pushMatrix();
        mc.gameRenderer.enableLightMap();
        try {
            renderingEsp = true;
            // Vanilla re-enables depth testing for opaque text (and keeps it on
            // for sneaking labels). ALWAYS covers both paths without a huge
            // polygon offset. Label mixins keep depth writes off during this pass.
            GlStateManager.depthFunc(GL11.GL_ALWAYS);
            GlStateManager.depthMask(false);
            for (int i = 0, count = candidates.size(); i < count; i++) {
                PlayerEntity player = candidates.get(i);
                int light = player.isOnFire() ? 15728880 : player.getLightLevel(tickDelta);
                GLX.multiTexCoord2f(GLX.GL_TEXTURE1, light % 65536, light / 65536);
                double x = player.prevX + (player.x - player.prevX) * tickDelta - px;
                double y = player.prevY + (player.y - player.prevY) * tickDelta - py;
                double z = player.prevZ + (player.z - player.prevZ) * tickDelta - pz;
                EntityRenderer<?> renderer = dispatcher.getRenderer(player);
                if (renderer instanceof LivingEntityRenderer) {
                    // The normal renderer preserves scoreboard rules and inline nametag mods.
                    ((LivingEntityRenderer<PlayerEntity>) renderer).renderNameTag(player, x, y, z);
                }
            }
        } finally {
            renderingEsp = false;
            // Keep capacity for the next frame without retaining departed players.
            candidates.clear();
            previous.restoreLightmap();
            mc.gameRenderer.disableLightMap();
            GlStateManager.popMatrix();
            previous.restore();
        }
    }

    /** Restore GL state through Minecraft's cache after the label-only pass. */
    private static final class RenderState {
        private int activeTexture, depthFunc, srcRgb, dstRgb, srcAlpha, dstAlpha;
        private boolean lighting, blend, depth, depthMask, texture, lightmapTexture;
        private final FloatBuffer color = BufferUtils.createFloatBuffer(4);
        private final FloatBuffer light = BufferUtils.createFloatBuffer(4);

        private void capture() {
            activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            color.clear();
            light.clear();
            GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, color);
            GlStateManager.activeTexture(GLX.GL_TEXTURE1);
            lightmapTexture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            GL11.glGetFloatv(GL11.GL_CURRENT_TEXTURE_COORDS, light);
            GlStateManager.activeTexture(activeTexture);
        }

        private void restoreLightmap() {
            GLX.multiTexCoord2f(GLX.GL_TEXTURE1, light.get(0), light.get(1));
        }

        private void restore() {
            GlStateManager.activeTexture(GLX.GL_TEXTURE1);
            if (lightmapTexture) GlStateManager.enableTexture(); else GlStateManager.disableTexture();
            GlStateManager.activeTexture(GLX.GL_TEXTURE0);
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (depth) GlStateManager.enableDepthTest(); else GlStateManager.disableDepthTest();
            if (texture) GlStateManager.enableTexture(); else GlStateManager.disableTexture();
            GlStateManager.depthMask(depthMask);
            GlStateManager.depthFunc(depthFunc);
            GlStateManager.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GlStateManager.color4f(color.get(0), color.get(1), color.get(2), color.get(3));
            GlStateManager.activeTexture(activeTexture);
        }
    }

}
