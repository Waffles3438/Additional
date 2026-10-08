package me.waffles.additional.mixin;

import me.waffles.additional.Additional;
import me.waffles.additional.render.NameTagESP;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Unique
    private final NameTagESP additional$nametags = new NameTagESP();

    // Draw while the world projection is active, before the hand switches it.
    @Inject(method = "render(IFJ)V", at = @At(value = "CONSTANT", args = "stringValue=hand"))
    private void additional$renderNametags(int pass, float tickDelta, long finishTimeNano, CallbackInfo ci) {
        if (Additional.config != null) additional$nametags.onRenderWorld(tickDelta);
    }
}
