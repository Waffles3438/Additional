package me.waffles.additional.mixin;

import me.waffles.additional.Additional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void additional$tickStart(CallbackInfo ci) { Additional.tickStart(); }

    @Inject(method = "tick", at = @At("RETURN"))
    private void additional$tickEnd(CallbackInfo ci) { Additional.tickEnd(); }

    @Inject(method = "setWorld(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V", at = @At("HEAD"))
    private void additional$worldChanged(ClientWorld world, String message, CallbackInfo ci) {
        Additional.worldChanged();
    }
}
