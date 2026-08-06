package me.waffles.additional.mixin;

import me.waffles.additional.util.BotUtils;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import me.waffles.additional.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(value = Render.class)
public class RenderMixin {

    @Inject(method = "renderLivingLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;disableDepth()V"))
    private void enableOffsetFill(Entity entity, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if(ModConfig.nametagsThroughWalls && !BotUtils.isBot(entity) && ModConfig.masterSwitch) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1.0f, -Float.MAX_VALUE);
        }
    }

    @Inject(method = "renderLivingLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableLighting()V"))
    private void disableOffsetFill(Entity entity, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if(ModConfig.nametagsThroughWalls && !BotUtils.isBot(entity) && ModConfig.masterSwitch) {
            glPolygonOffset(0.0f, 0.0f);
            glDisable(GL_POLYGON_OFFSET_FILL);
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
        if(ModConfig.extendNametagRange && !BotUtils.isBot(entityIn) && ModConfig.masterSwitch) {
            return 0.0D;
        }
        return entityIn.getDistanceSqToEntity(instance);
    }
}
