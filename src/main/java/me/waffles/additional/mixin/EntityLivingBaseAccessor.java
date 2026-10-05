package me.waffles.additional.mixin;

import net.minecraft.entity.living.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface EntityLivingBaseAccessor {
    @Accessor("jumpingCooldown")
    void setJumpTicks(int jumpTicks);

    @Accessor("jumpingCooldown")
    int getJumpTicks();
}
