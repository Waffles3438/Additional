package me.waffles.additional.mixin;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLivingBase.class)
public interface EntityLivingBaseAccessor {
    @Accessor
    void setJumpTicks(int jumpTicks);

    @Accessor
    int getJumpTicks();
}
