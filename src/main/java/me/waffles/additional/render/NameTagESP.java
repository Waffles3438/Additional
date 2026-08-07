package me.waffles.additional.render;

import me.waffles.additional.config.ModConfig;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NameTagESP {

    private final Minecraft mc = Minecraft.getMinecraft();

    public static final Set<UUID> renderedPlayers = new HashSet<>();

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            renderedPlayers.clear();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!ModConfig.masterSwitch || !ModConfig.nametagsThroughWalls) return;

        EntityPlayer viewer = mc.thePlayer;
        if (viewer == null || mc.theWorld == null) return;

        // Gather only the players the vanilla pass already culled (those marked in
        // renderedPlayers are skipped). If nothing is left we can bail before
        // touching the lightmap or re-caching the render pipeline at all.
        List<EntityPlayer> candidates = null;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == viewer) continue;
            if (BotUtils.isBot(player)) continue;
            if (renderedPlayers.contains(player.getUniqueID())) continue;
            if (candidates == null) candidates = new ArrayList<EntityPlayer>(4);
            candidates.add(player);
        }
        if (candidates == null) return;

        Entity camera = mc.getRenderViewEntity();
        if (camera == null) camera = viewer;

        float pt = event.partialTicks;
        double px = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * pt;
        double py = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * pt;
        double pz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * pt;

        RenderManager rm = mc.getRenderManager();

        // RenderLib replaces the vanilla RenderGlobal.renderEntities call, so
        // RenderManager.cacheActiveRenderInfo is never invoked and livingPlayer/
        // textRenderers stay null - renderName() would NPE. Re-cache with the
        // current camera so the label distance check and font rendering work.
        rm.cacheActiveRenderInfo(mc.theWorld, mc.fontRendererObj, camera, mc.pointedEntity, mc.gameSettings, pt);

        // Mirror PolyNametag.onRender: by default it dims every nametag using the
        // tagged player's own brightness via the lightmap. Entities left to this
        // event (culled behind walls) never get that handling, so they would stay
        // fullbright. Enable the lightmap and apply per-player coords here so a
        // nametag looks identical whether or not it is behind a wall.
        //
        // The reflection behind usingDirectRender only toggles once per frame,
        // so set it once for the whole loop instead of once per player.
        mc.entityRenderer.enableLightmap();
        PolyNametagCompat.usingDirectRender(true);
        try {
            for (EntityPlayer player : candidates) {
                int brightness = player.isBurning() ? 15728880 : player.getBrightnessForRender(pt);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                        (float) (brightness % 65536), (float) (brightness / 65536));

                double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt - px;
                double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt - py;
                double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt - pz;

                Render render = rm.getEntityRenderObject(player);
                if (!(render instanceof RendererLivingEntity)) continue;

                ((RendererLivingEntity<EntityLivingBase>) render).renderName(player, x, y, z);
            }
        } finally {
            PolyNametagCompat.usingDirectRender(false);
            mc.entityRenderer.disableLightmap();
        }
    }
}