package me.waffles.additional.mixin;

import me.waffles.additional.config.ModConfig;
import me.waffles.additional.Additional;
import me.waffles.additional.render.NameTagESP;
import me.waffles.additional.render.PlayerNametagCache;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.scoreboard.team.AbstractTeam;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityRenderer.class, priority = 1100)
public class RenderLivingEntityMixin {

    @Redirect(method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TextRenderer;getWidth(Ljava/lang/String;)I"))
    private int additional$reuseSneakingLabelWidth(TextRenderer renderer, String text) {
        return NameTagESP.getTextWidth(renderer, text);
    }

    @Redirect(method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/living/LivingEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private Text additional$cachedPlayerName(LivingEntity entity) {
        return entity instanceof PlayerEntity
                ? ((PlayerNametagCache) entity).additional$getNametagText(Additional.nametagTextTick)
                : entity.getDisplayName();
    }

    @Inject(
            method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At("HEAD"), cancellable = true
    )
    private void deferEspLabel(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
        // Draw eligible player labels once, after terrain, translucent blocks and
        // weather. The late pass calls this renderer again with the ESP pass active.
        if (NameTagESP.shouldDefer(entity)) ci.cancel();
    }

    // Vanilla has accepted visibility and range before reaching alphaFunc.
    // Delay per-player light calculation until here instead of repeating those checks.
    @Inject(method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;alphaFunc(IF)V"))
    private void prepareEspLabel(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
        NameTagESP.prepareLabel(entity);
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
        if(ModConfig.masterSwitch && ModConfig.invisNametags && !BotUtils.isBot(instance)) {
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
        return instance.isSneaking()
                && (!ModConfig.masterSwitch || !ModConfig.nametagsOnShift || BotUtils.isBot(instance));
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
        return !entityPlayer.isSpectator() && instance.isInvisible();
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
