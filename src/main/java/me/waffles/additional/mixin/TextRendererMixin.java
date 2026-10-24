package me.waffles.additional.mixin;

import me.waffles.additional.render.NameTagESP;
import net.minecraft.client.render.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
    // Hook the method every label text draw reaches: vanilla's int overload
    // delegates here, and PolyNametag calls it directly from its wrappers.
    @Inject(method = "draw(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void additional$skipHiddenEspText(String text, float x, float y, int color, boolean shadow,
                                              CallbackInfoReturnable<Integer> cir) {
        // Label code discards this width, so returning 0 is safe.
        if (NameTagESP.consumeHiddenText()) cir.setReturnValue(0);
    }
}
