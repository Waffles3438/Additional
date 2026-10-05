plugins {
    java
    id("fabric-loom") version "1.17.21"
    id("ploceus") version "1.17.7"
}

group = "me.waffles"
version = "${property("mod.version")}+mc${property("minecraft.version")}-ornithe"
base.archivesName.set(property("mod.id").toString())

repositories {
    mavenCentral()
    maven("https://repo.polyfrost.org/releases")
    maven("https://maven.cloverclient.com/releases")
    maven("https://maven.fabricmc.net")
    maven("https://libraries.minecraft.net")
    maven("https://maven.google.com")
}

ploceus.setIntermediaryGeneration(2)

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft.version")}")
    mappings(ploceus.featherMappings(property("feather.build").toString()))
    modImplementation("net.fabricmc:fabric-loader:${property("loader.version")}")
    ploceus.dependOsl(property("osl.version").toString())
    modImplementation("org.polyfrost.oneconfig:1.8.9-ornithe:${property("oneconfig.version")}")
    testImplementation("junit:junit:4.13.2")
}

// OneConfig's Ornithe platform uses Pylon/LWJGL 3.
configurations.configureEach { exclude(group = "org.lwjgl.lwjgl") }

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    val metadata = mapOf("version" to project.version, "oneconfig_version" to project.property("oneconfig.version"))
    inputs.properties(metadata)
    filesMatching("fabric.mod.json") { expand(metadata) }
}

loom {
    runs.named("client") { property("mixin.debug.countInjections", "true") }
    runs.remove(runs.getByName("server"))
}
