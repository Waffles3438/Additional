package me.waffles.additional.mixin;

import me.waffles.additional.util.BotUtils;
import me.waffles.additional.render.NameTagESP;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import me.waffles.additional.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityRenderer.class)
public class RenderMixin {

    @Redirect(method = "renderNameTag(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TextRenderer;getWidth(Ljava/lang/String;)I"))
    private int additional$reuseLabelWidth(TextRenderer renderer, String text) {
        return NameTagESP.getTextWidth(renderer, text);
    }

    @Redirect(
            method = "renderNameTag(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;squaredDistanceTo(Lnet/minecraft/entity/Entity;)D"
            )
    )
    private double extendNametagRange(Entity entityIn, Entity instance) {
        if(ModConfig.masterSwitch && ModConfig.extendNametagRange && !BotUtils.isBot(entityIn)) {
            return 0.0D;
        }
        return entityIn.squaredDistanceTo(instance);
    }
}
