package me.waffles.additional.render;

import me.waffles.additional.config.ModConfig;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
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
        if (!ModConfig.masterSwitch || !ModConfig.nametagsThroughWalls)
            return;

        float pt = event.partialTicks;

        EntityPlayer viewer = mc.thePlayer;
        if (viewer == null || mc.theWorld == null)
            return;

        RenderManager rm = mc.getRenderManager();

        double px = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * pt;
        double py = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * pt;
        double pz = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * pt;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == viewer) continue;
            if (BotUtils.isBot(player)) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt - px;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt - py;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt - pz;

            Render render = rm.getEntityRenderObject(player);
            if (render instanceof RendererLivingEntity) {

                if (renderedPlayers.contains(player.getUniqueID()))
                    continue;

                ((RendererLivingEntity<EntityLivingBase>) render)
                        .renderName(player, x, y, z);
            }
        }
    }
}