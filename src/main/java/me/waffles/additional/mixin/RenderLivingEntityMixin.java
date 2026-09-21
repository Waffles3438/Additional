package me.waffles.additional.mixin;

import me.waffles.additional.config.ModConfig;
import me.waffles.additional.render.NameTagESP;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.scoreboard.team.AbstractTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityRenderer.class, priority = 1100)
public class RenderLivingEntityMixin {

    @Inject(
            method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At("HEAD")
    )
    private void markEspRendered(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
        // Only the master-on through-wall fallback uses this inferred marker. Legit
        // Mode receives the exact culling result from its optional integration.
        if (ModConfig.masterSwitch && ModConfig.nametagsThroughWalls && entity instanceof PlayerEntity) {
            NameTagESP.renderedPlayers.add(entity.getUuid());
        }
    }

    @Inject(
            method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;FD)V")
    )
    private void shiftNameTagsWhileSneakingHead(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
        if(!(entity instanceof PlayerEntity)) return;
        // Legit Mode preserves vanilla player nametag positioning. The predicate is
        // false while the master switch is on, so ESP behavior still takes priority.
        if(!ModConfig.isLegitModeActive() && entity.isSneaking()) {
            GlStateManager.pushMatrix();
            GlStateManager.translatef(0.0F, -0.25F, 0.0F);
        }
    }

    @Inject(
            method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/entity/Entity;DDDLjava/lang/String;FD)V")
    )
    private void shiftNameTagsWhileSneakingTail(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
        if(!(entity instanceof PlayerEntity)) return;
        // Keep this condition identical to the head injection so the GL matrix stack
        // remains balanced in every rendering mode.
        if(!ModConfig.isLegitModeActive() && entity.isSneaking()) {
            GlStateManager.popMatrix();
        }
    }
    
    @Redirect(
            method = "shouldRenderNameTag(Lnet/minecraft/entity/living/LivingEntity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/living/LivingEntity;getScoreboardTeam()Lnet/minecraft/scoreboard/team/AbstractTeam;"
            )
    )
    private AbstractTeam showInvis(LivingEntity instance) {
        if(ModConfig.invisNametags && !BotUtils.isBot(instance) && ModConfig.masterSwitch) {
            return null;
        }
        return instance.getScoreboardTeam();
    }
    
    @Redirect(
            method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/living/LivingEntity;isSneaking()Z"
            )
    )
    private boolean cancel(LivingEntity instance) {
        if(BotUtils.isBot(instance)) return instance.isSneaking();
        return !(ModConfig.nametagsOnShift && ModConfig.masterSwitch) && instance.isSneaking();
    }

    @Redirect(
            method = "shouldRenderNameTag(Lnet/minecraft/entity/living/LivingEntity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/living/LivingEntity;isInvisibleTo(Lnet/minecraft/entity/living/player/PlayerEntity;)Z"
            )
    )
    private boolean showInvisible(LivingEntity instance, PlayerEntity entityPlayer) {
        if(ModConfig.invisNametags && ModConfig.masterSwitch && instance.isInvisible() && !BotUtils.isBot(instance)) {
            return false;
        }
        return instance.isInvisibleTo(entityPlayer);
    }

    @ModifyVariable(
            method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At("STORE"),
            ordinal = 0
    )
    public float extendNametagRange(float range) {
        if (ModConfig.extendNametagRange && ModConfig.masterSwitch) {
            return 256F;
        }
        return range;
    }
}
