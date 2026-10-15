package me.waffles.additional.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.waffles.additional.config.ModConfig;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

// Apply after PolyNametag adds its handlers to the vanilla renderer. No PolyNametag
// classes are linked, and the optional hook is skipped when it is not installed.
@Mixin(value = EntityRenderer.class, priority = 900)
public abstract class PolyNametagCompatMixin {
    @Dynamic("PolyNametag's optional sneaking visibility handler")
    @TargetHandler(mixin = "org.polyfrost.polynametag.mixin.client.Mixin_WrapNametagRender",
            name = "polynametag$occludeSneakingNametag")
    @ModifyReturnValue(method = "@MixinSquared:Handler", at = @At("RETURN"),
            remap = false, require = 0, expect = 0)
    private boolean additional$showThroughWalls(boolean renderNormally, @Local(argsOnly = true) Entity entity) {
        return renderNormally || additional$showSneakingName(entity);
    }

    @Dynamic("PolyNametag's optional sneaking text handler")
    @TargetHandler(mixin = "org.polyfrost.polynametag.mixin.client.Mixin_WrapNametagRender",
            name = "polynametag$skipOpaqueSneakingPass")
    @ModifyReturnValue(method = "@MixinSquared:Handler", at = @At("RETURN"),
            remap = false, require = 0, expect = 0)
    private boolean additional$showOpaqueText(boolean renderNormally, @Local(argsOnly = true) Entity entity) {
        return renderNormally || additional$showSneakingName(entity);
    }

    @Unique
    private static boolean additional$showSneakingName(Entity entity) {
        // Preserve PolyNametag's separate height adjustment. Changing its shared
        // sneakLook helper also raises the label when this option is enabled.
        return ModConfig.masterSwitch && ModConfig.nametagsOnShift
                && entity instanceof PlayerEntity && !BotUtils.isBot(entity);
    }
}
