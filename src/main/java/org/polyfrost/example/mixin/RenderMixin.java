package org.polyfrost.example.mixin;

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
