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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NameTagESP {
    private final Minecraft mc = Minecraft.getInstance();
    private Culler frustum;
    public static final Set<UUID> renderedPlayers = new HashSet<>();

    public static void clearFrame() { renderedPlayers.clear(); }

    @SuppressWarnings("unchecked")
    public void onRenderWorld(float tickDelta) {
        if (!ModConfig.masterSwitch || !ModConfig.nametagsThroughWalls) return;
        PlayerEntity viewer = mc.player;
        if (viewer == null || mc.world == null) return;

        List<PlayerEntity> candidates = null;
        for (PlayerEntity player : mc.world.players) {
            if (player == viewer || player.removed || player.deathTicks > 0 || !player.inChunk) continue;
            // Chunk membership and death animation state prevent stale, shaking labels.
            // Health is deliberately omitted: servers can report fake health for players.
            if (renderedPlayers.contains(player.getUuid()) || BotUtils.isBot(player)) continue;
            if (candidates == null) candidates = new ArrayList<>(4);
            candidates.add(player);
        }
        if (candidates == null) return;

        Entity camera = mc.getCamera();
        if (camera == null) camera = viewer;
        double px = camera.prevX + (camera.x - camera.prevX) * tickDelta;
        double py = camera.prevY + (camera.y - camera.prevY) * tickDelta;
        double pz = camera.prevZ + (camera.z - camera.prevZ) * tickDelta;

        // Match vanilla's frustum test before paying for a formatted label.
        Frustum.getInstance();
        if (frustum == null) frustum = new FrustumCuller();
        frustum.prepare(px, py, pz);
        int visible = 0;
        for (PlayerEntity player : candidates) {
            if (frustum.isVisible(player.getShape())) candidates.set(visible++, player);
        }
        if (visible == 0) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.prepare(mc.world, mc.textRenderer, camera, mc.targetEntity, mc.options, tickDelta);
        RenderState previous = new RenderState();
        GlStateManager.pushMatrix();
        mc.gameRenderer.enableLightMap();
        try {
            for (int i = 0; i < visible; i++) {
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
            previous.restoreLightmap();
            mc.gameRenderer.disableLightMap();
            GlStateManager.popMatrix();
            previous.restore();
        }
    }

    /** Restore GL state through Minecraft's cache after the label-only pass. */
    private static final class RenderState {
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
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
        private final boolean lightmapTexture;

        private RenderState() {
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
            GlStateManager.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GlStateManager.color4f(color.get(0), color.get(1), color.get(2), color.get(3));
            GlStateManager.activeTexture(activeTexture);
        }
    }

}
