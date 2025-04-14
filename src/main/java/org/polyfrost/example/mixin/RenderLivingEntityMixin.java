package org.polyfrost.example.mixin;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import org.polyfrost.example.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RendererLivingEntity.class)
public class RenderLivingEntityMixin {

    @Inject(
            method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderOffsetLivingLabel(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;FD)V")
    )
    private void shiftNameTagsWhileSneakingHead(EntityLivingBase entity, double x, double y, double z, CallbackInfo ci) {
        if(!(entity instanceof EntityPlayer)) return;
        if(entity.isSneaking()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, -0.25F, 0.0F);
        }
    }

    @Inject(
            method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderOffsetLivingLabel(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;FD)V")
    )
    private void shiftNameTagsWhileSneakingTail(EntityLivingBase entity, double x, double y, double z, CallbackInfo ci) {
        if(!(entity instanceof EntityPlayer)) return;
        if(entity.isSneaking()) {
            GlStateManager.popMatrix();
        }
    }
    
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
        if(isBot(instance)) return instance.isSneaking();
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
        return !entityPlayer.isSpectator() && instance.isInvisible();
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
        if (entity.getUniqueID().version() == 2 ||
                entity instanceof EntityPlayer &&
                        (entity.getName().contains("[NPC]")
                                || entity.getName().contains("[BOT]")
                                || entity.getName().contains("iAT3")
                                || entity.getName() == null
                                || entity.getName().contains("npc-")
                                || (entity.getName().contains("§") && (entity.getName().contains("SHOP") || entity.getName().contains("UPGRADE"))))
        ) {
            return true;
        } else {
            return !(entity instanceof EntityPlayer);
        }
    }
}
