package me.waffles.additional.mixin;

import me.waffles.additional.config.ModConfig;
import me.waffles.additional.render.NameTagESP;
import me.waffles.additional.util.BotUtils;
import me.waffles.additional.util.ClientTasks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void additional$tick(CallbackInfo ci) {
        ClientTasks.flushChat();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && ModConfig.ndj) {
            EntityLivingBaseAccessor player = (EntityLivingBaseAccessor) client.player;
            if (player.getJumpTicks() > ModConfig.jumpTicks) player.setJumpTicks(ModConfig.jumpTicks);
        }
    }

    // This overload handles both world transfers and disconnects (a null world).
    @Inject(method = "setWorld(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V", at = @At("HEAD"))
    private void additional$worldChanged(CallbackInfo ci) {
        BotUtils.clearCache();
        NameTagESP.beginFrame();
    }
}
