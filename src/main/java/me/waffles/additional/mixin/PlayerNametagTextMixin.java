package me.waffles.additional.mixin;

import me.waffles.additional.Additional;
import me.waffles.additional.render.PlayerNametagCache;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Sarcio replaces this same call to avoid creating a Text component.
// AdditionalMixinPlugin omits only this optimization when Sarcio is installed.
@Mixin(value = LivingEntityRenderer.class, priority = 1100)
public abstract class PlayerNametagTextMixin {
    @Redirect(method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/living/LivingEntity;getDisplayName()Lnet/minecraft/text/Text;"))
    private Text additional$cachedPlayerName(LivingEntity entity) {
        return entity instanceof PlayerEntity
                ? ((PlayerNametagCache) entity).additional$getNametagText(Additional.nametagTextTick)
                : entity.getDisplayName();
    }
}
