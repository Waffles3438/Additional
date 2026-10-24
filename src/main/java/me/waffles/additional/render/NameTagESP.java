package me.waffles.additional.render;

import me.waffles.additional.config.ModConfig;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.FrustumCuller;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.platform.GLX;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;
import java.util.List;

public class NameTagESP {
    private final Minecraft mc = Minecraft.getInstance();
    private final RenderState previous = new RenderState();
    private Culler worldFrustum;
    private Culler fallbackFrustum;
    private float passTickDelta;
    private TextRenderer widthRenderer;
    private String widthText;
    private int textWidth;
    private boolean hideNextText;
    private static NameTagESP activePass;
    private static boolean dispatcherPrepared;

    public static boolean isRenderingEsp() { return activePass != null; }

    public static int getTextWidth(TextRenderer renderer, String text) {
        NameTagESP pass = activePass;
        if (pass == null) return renderer.getWidth(text);
        // Vanilla measures the same string three times for one label. Keep this
        // cache within the pass so resource/font changes are never held across frames.
        if (pass.widthRenderer != renderer || !java.util.Objects.equals(pass.widthText, text)) {
            pass.textWidth = renderer.getWidth(text);
            pass.widthRenderer = renderer;
            pass.widthText = text;
        }
        return pass.textWidth;
    }

    public void beginWorldPass() {
        // Each anaglyph eye needs its own prepared frustum and dispatcher state.
        worldFrustum = null;
        dispatcherPrepared = false;
    }

    public void setWorldFrustum(Culler culler) { worldFrustum = culler; }

    public static void markDispatcherPrepared() { dispatcherPrepared = true; }

    public static void prepareLabel(LivingEntity entity) {
        if (activePass != null) activePass.prepareAcceptedLabel(entity);
    }

    private void prepareAcceptedLabel(LivingEntity entity) {
        int light = entity.isOnFire() ? 15728880 : entity.getLightLevel(passTickDelta);
        GLX.multiTexCoord2f(GLX.GL_TEXTURE1, light % 65536, light / 65536);
    }

    /** True when Additional draws sneaking player labels the same as standing ones. */
    public static boolean showsSneakingName(Entity entity) {
        return ModConfig.masterSwitch && ModConfig.nametagsOnShift
                && entity instanceof PlayerEntity && !BotUtils.isBot(entity);
    }

    /** Called when a label starts drawing, before its background and text. */
    public static void beginLabel(Entity entity) {
        NameTagESP pass = activePass;
        if (pass == null) return;
        // A label draws its text twice: a faint copy with depth testing off, then
        // an opaque copy. Sneaking labels can be drawn as the faint copy alone
        // (vanilla and PolyNametag), so keep it unless the opaque copy is certain.
        pass.hideNextText = !entity.isSneaking() || showsSneakingName(entity);
    }

    /**
     * Skips the faint copy of an ESP label's text. The pass draws with
     * depthFunc ALWAYS, so the opaque copy lands on the same pixels and covers it.
     */
    public static boolean consumeHiddenText() {
        NameTagESP pass = activePass;
        if (pass == null || !pass.hideNextText) return false;
        pass.hideNextText = false;
        // Only the faint copy is drawn with depth testing disabled.
        return !GlStateManager.DEPTH.state.enabled;
    }

    public static boolean shouldDefer(Entity entity) {
        return !isRenderingEsp() && ModConfig.masterSwitch && ModConfig.nametagsThroughWalls
                && entity instanceof PlayerEntity && !BotUtils.isBot(entity);
    }

    @SuppressWarnings("unchecked")
    public void onRenderWorld(float tickDelta) {
        if (!ModConfig.masterSwitch || !ModConfig.nametagsThroughWalls) return;
        PlayerEntity viewer = mc.player;
        if (viewer == null || mc.world == null) return;

        Entity camera = mc.getCamera();
        if (camera == null) camera = viewer;
        double px = camera.prevX + (camera.x - camera.prevX) * tickDelta;
        double py = camera.prevY + (camera.y - camera.prevY) * tickDelta;
        double pz = camera.prevZ + (camera.z - camera.prevZ) * tickDelta;
        boolean extendedRange = ModConfig.extendNametagRange;
        boolean showSneaking = ModConfig.nametagsOnShift;
        Culler frustum = worldFrustum;
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        List<PlayerEntity> players = mc.world.players;
        boolean passStarted = false;
        Object argentumBatch = null;
        passTickDelta = tickDelta;
        try {
            // World/player updates run on this client thread outside rendering.
            // Filter and draw in one scan without retaining a candidate list.
            for (int i = 0, count = players.size(); i < count; i++) {
                PlayerEntity player = players.get(i);
                if (player == viewer || player.removed || player.deathTicks > 0 || !player.inChunk) continue;
                // Servers can report fake health, so use removal/death/chunk state.
                double rangeSquared = extendedRange ? 65536.0
                        : (!showSneaking && player.isSneaking() ? 1024.0 : 4096.0);
                if (player.squaredDistanceTo(camera) >= rangeSquared || BotUtils.isBot(player)) continue;
                if (frustum == null) {
                    // Render paths that bypass vanilla's setupRender still work.
                    if (fallbackFrustum == null) fallbackFrustum = new FrustumCuller();
                    else Frustum.getInstance();
                    frustum = fallbackFrustum;
                    frustum.prepare(px, py, pz);
                }
                if (!frustum.isVisible(player.getShape())) continue;
                EntityRenderer<?> renderer = dispatcher.getRenderer(player);
                if (!(renderer instanceof LivingEntityRenderer)) continue;
                if (!passStarted) {
                    if (!dispatcherPrepared) {
                        dispatcher.prepare(mc.world, mc.textRenderer, camera, mc.targetEntity, mc.options, tickDelta);
                    }
                    previous.capture();
                    GlStateManager.pushMatrix();
                    passStarted = true;
                    activePass = this;
                    mc.gameRenderer.enableLightMap();
                    // GlStateManagerMixin protects depth writes in every renderer,
                    // including PolyNametag's sneaking and shadow paths.
                    GlStateManager.depthFunc(GL11.GL_ALWAYS);
                    GlStateManager.depthMask(false);
                    // Argentum batches labels only inside its entity pass, which
                    // has already flushed. Reopen it so ESP labels share its draws.
                    argentumBatch = ArgentumNameTags.begin();
                }
                double x = player.prevX + (player.x - player.prevX) * tickDelta - px;
                double y = player.prevY + (player.y - player.prevY) * tickDelta - py;
                double z = player.prevZ + (player.z - player.prevZ) * tickDelta - pz;
                // Preserve native visibility/scoreboard rules and nametag mods.
                ((LivingEntityRenderer<PlayerEntity>) renderer).renderNameTag(player, x, y, z);
            }
        } finally {
            if (passStarted) {
                // Flush while the ESP depth and lightmap state are still active.
                ArgentumNameTags.end(argentumBatch);
                activePass = null;
                hideNextText = false;
                widthRenderer = null;
                widthText = null;
                previous.restoreLightmap();
                mc.gameRenderer.disableLightMap();
                GlStateManager.popMatrix();
                previous.restore();
            }
        }
    }

    /** Restore GL state through Minecraft's cache after the label-only pass. */
    private static final class RenderState {
        private int activeTexture, depthFunc, srcRgb, dstRgb, srcAlpha, dstAlpha;
        private boolean lighting, blend, depth, depthMask, texture, lightmapTexture;
        private final FloatBuffer color = BufferUtils.createFloatBuffer(4);
        private final FloatBuffer light = BufferUtils.createFloatBuffer(4);

        private void capture() {
            // These states are tracked by Minecraft and restored through the
            // same cache. Color and light coordinates can be changed directly
            // by vertex/renderer code. Vanilla blendFunc also leaves its cached
            // alpha factors stale, so query those four actual GL values.
            activeTexture = GLX.GL_TEXTURE0 + GlStateManager.texture;
            lighting = GlStateManager.LIGHTING.enabled;
            blend = GlStateManager.BLEND.state.enabled;
            depth = GlStateManager.DEPTH.state.enabled;
            depthMask = GlStateManager.DEPTH.mask;
            depthFunc = GlStateManager.DEPTH.func;
            texture = GlStateManager.TEXTURES[0].state.enabled;
            lightmapTexture = GlStateManager.TEXTURES[1].state.enabled;
            srcRgb = GlStateManager.BLEND.sfactorRGB;
            dstRgb = GlStateManager.BLEND.dfactorRGB;
            srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            color.clear();
            light.clear();
            GL11.glGetFloatv(GL11.GL_CURRENT_COLOR, color);
            GlStateManager.activeTexture(GLX.GL_TEXTURE1);
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
