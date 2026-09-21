package me.waffles.additional.mixin;

import me.waffles.additional.render.NameTagESP;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void additional$beginEntityPass(Entity camera, Culler culler, float tickDelta, CallbackInfo ci) {
        NameTagESP.beginFrame();
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;disableLightMap()V"))
    private void additional$renderMissingLabels(Entity camera, Culler culler, float tickDelta, CallbackInfo ci) {
        NameTagESP.render(camera, culler, tickDelta);
    }
}
