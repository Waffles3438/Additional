package org.polyfrost.example.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.polyfrost.example.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(value = Render.class)
public class RenderMixin {

    @Redirect(
            method = "renderLivingLabel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GlStateManager;depthMask(Z)V"
            )
    )
    private void disabledepth_and_depthmask(boolean flag){
        if(ModConfig.nametagsThroughWalls && flag){
            GlStateManager.disableDepth();
        }
        GlStateManager.depthMask(flag);
    }

    @Inject(
            method = "renderLivingLabel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GlStateManager;enableLighting()V"
            )
    )
    private void enabledepth(Entity entityIn, String str, double x, double y, double z, int maxDistance, CallbackInfo ci){
        if(ModConfig.nametagsThroughWalls){
            GlStateManager.enableDepth();
        }
    }

    @Redirect(
            method = "renderLivingLabel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getDistanceSqToEntity(Lnet/minecraft/entity/Entity;)D"
            )
    )
    private double extendNametagRange(Entity entityIn, Entity instance) {
        if(ModConfig.extendNametagRange && !isBot(entityIn)) {
            return 0.0D;
        }
        return entityIn.getDistanceSqToEntity(instance);
    }

    public boolean isBot(Entity entity){
        if (entity instanceof EntityPlayer && ((((EntityPlayer) entity).getDisplayNameString().contains("§c") && (entity.getUniqueID().version() == 2))
                || ((EntityPlayer) entity).getDisplayNameString().contains("[NPC]")
                || ((EntityPlayer) entity).getDisplayNameString().contains("[BOT]")
                || ((EntityPlayer) entity).getDisplayNameString().contains("iAT3")
                || ((EntityPlayer) entity).getDisplayNameString().isEmpty()
                || (entity.getUniqueID().version() == 2)
                || ((EntityPlayer) entity).getDisplayNameString().contains("§") && (((EntityPlayer) entity).getDisplayNameString().contains("SHOP") || ((EntityPlayer) entity).getDisplayNameString().contains("UPGRADE")))) {
            return true;
        } else {
            for (String name : getAllPlayerNamesFromTabList()) {
                if (entity instanceof EntityPlayer && ((EntityPlayer) entity).getDisplayNameString().contains(name)) {
                    return false;
                }
            }
            return true;
        }
    }

    public ArrayList<String> getAllPlayerNamesFromTabList() {
        ArrayList<String> playerNames = new ArrayList<>();
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();

        if (netHandler != null) {
            for (NetworkPlayerInfo info : netHandler.getPlayerInfoMap()) {
                playerNames.add(info.getGameProfile().getName());
            }
        }

        return playerNames;
    }

}
