package me.waffles.additional.mixin;

import me.waffles.additional.render.NameTagESP;
import net.minecraft.client.render.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GlStateManager.class)
public abstract class GlStateManagerMixin {
    // Keep the entire ESP pass read-only, including PolyNametag's shadow helper.
    // Intercept before Minecraft caches the mask so its cache stays consistent.
    @ModifyVariable(method = "depthMask(Z)V", at = @At("HEAD"), argsOnly = true)
    private static boolean additional$keepEspDepthReadOnly(boolean mask) {
        return mask && !NameTagESP.isRenderingEsp();
    }
}
