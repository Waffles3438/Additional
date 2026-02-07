import dev.deftu.gradle.utils.GameSide

plugins {
    id("java")
    id("dev.deftu.gradle.tools") version("2.69.+")
    id("dev.deftu.gradle.tools.resources") version("2.69.+")
    id("dev.deftu.gradle.tools.bloom") version("2.69.+")
    id("dev.deftu.gradle.tools.shadow") version("2.69.+")
    id("dev.deftu.gradle.tools.minecraft.loom") version("2.69.+")
}

repositories {
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.polyfrost.org/snapshots")
}

dependencies {
    compileOnly("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")

    shade("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+")
    implementation("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+")

    compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT")
}

toolkitLoomHelper {
    useMixinRefMap(modData.id)
    useForgeMixin(modData.id)

    useTweaker("cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")

    useDevAuth("+")
    useProperty("mixin.debug.export", "true", GameSide.CLIENT)
    disableRunConfigs(GameSide.SERVER)
}