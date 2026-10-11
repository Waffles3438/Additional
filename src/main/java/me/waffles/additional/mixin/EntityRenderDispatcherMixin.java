package me.waffles.additional.mixin;

import me.waffles.additional.render.NameTagESP;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "prepare", at = @At("RETURN"))
    private void additional$dispatcherPrepared(CallbackInfo ci) {
        NameTagESP.markDispatcherPrepared();
    }
}
