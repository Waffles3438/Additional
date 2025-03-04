package org.polyfrost.example.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import org.polyfrost.example.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(value = RendererLivingEntity.class)
public class RenderLivingEntityMixin {
    
    @Redirect(
            method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;getTeam()Lnet/minecraft/scoreboard/Team;"
            )
    )
    private Team showInvis(EntityLivingBase instance) {
        if(ModConfig.invisNametags && !isBot(instance)) {
            return null;
        }
        return instance.getTeam();
    }
    
    @Redirect(
            method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;isSneaking()Z"
            )
    )
    private boolean cancel(EntityLivingBase instance) {
        return !ModConfig.nametagsOnShift && instance.isSneaking();
    }

    @Redirect(
            method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;isInvisibleToPlayer(Lnet/minecraft/entity/player/EntityPlayer;)Z"
            )
    )
    private boolean showInvisible(EntityLivingBase instance, EntityPlayer entityPlayer) {
        if(ModConfig.invisNametags && instance.isInvisible() && !isBot(instance)) {
            return false;
        }
        return entityPlayer.isSpectator() ? false : instance.isInvisible();
    }

    @ModifyVariable(
            method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
            at = @At("STORE"),
            ordinal = 0
    )
    public float extendNametagRange(float range) {
        if (ModConfig.extendNametagRange) {
            return 256F;
        }
        return range;
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
