package me.waffles.additional;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class AdditionalMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        // OneConfig provides MixinSquared. Register its selector before our
        // optional PolyNametag hooks resolve the other mod's generated handlers.
        MixinSquaredBootstrap.init();
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Both optimizations redirect getDisplayName in the living renderer.
        // Leave that call to Sarcio while keeping Additional's rendering features.
        return !mixinClassName.equals("me.waffles.additional.mixin.PlayerNametagTextMixin")
                || !FabricLoader.getInstance().isModLoaded("sarcio");
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
