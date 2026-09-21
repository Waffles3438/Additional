package me.waffles.additional.mixin;

import me.waffles.additional.util.BotUtils;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import me.waffles.additional.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(value = EntityRenderer.class)
public class RenderMixin {

    @Inject(method = "renderNameTag(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;disableDepthTest()V"))
    private void enableOffsetFill(Entity entity, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if(ModConfig.nametagsThroughWalls && !BotUtils.isBot(entity) && ModConfig.masterSwitch) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            // The units term is scaled by the smallest resolvable depth difference before
            // being applied, so it only has to be large enough to clear a wall between the
            // tag and the camera - nowhere near the float maximum. -Float.MAX_VALUE
            // overflows the fixed-point depth representation drivers convert this into,
            // which is undefined and vendor dependent; a large finite value is predictable.
            glPolygonOffset(1.0f, -1000000000f);
        }
    }

    @Inject(method = "renderNameTag(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/platform/GlStateManager;enableLighting()V"))
    private void disableOffsetFill(Entity entity, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        if(ModConfig.nametagsThroughWalls && !BotUtils.isBot(entity) && ModConfig.masterSwitch) {
            glPolygonOffset(0.0f, 0.0f);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
    }

    @Redirect(
            method = "renderNameTag(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;squaredDistanceTo(Lnet/minecraft/entity/Entity;)D"
            )
    )
    private double extendNametagRange(Entity entityIn, Entity instance) {
        if(ModConfig.extendNametagRange && !BotUtils.isBot(entityIn) && ModConfig.masterSwitch) {
            return 0.0D;
        }
        return entityIn.squaredDistanceTo(instance);
    }
}
